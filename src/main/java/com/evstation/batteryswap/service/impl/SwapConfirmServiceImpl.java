package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.enums.ReservationStatus;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.InvoiceService;
import com.evstation.batteryswap.service.SwapConfirmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SwapConfirmServiceImpl implements SwapConfirmService {

    private final SwapTransactionRepository swapTransactionRepository;
    private final BatterySerialRepository batterySerialRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceService invoiceService;
    private final PlanTierRateRepository planTierRateRepository;
    private final ReservationRepository reservationRepository;
    private final com.evstation.batteryswap.service.BatteryHistoryService batteryHistoryService;

    @Override
    public String confirmSwap(Long transactionId, Long staffId,
            com.evstation.batteryswap.dto.request.StaffConfirmSwapRequest request) {

        // 🔍 1️⃣ Lấy giao dịch swap
        SwapTransaction tx = swapTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Swap transaction not found"));

        if (tx.getStatus() != SwapTransactionStatus.PENDING_CONFIRM) {
            throw new RuntimeException("This swap has already been processed");
        }

        // 2️⃣ Lấy pin cũ, trạm, và subscription
        BatterySerial oldBattery = tx.getBatterySerial();
        Station station = tx.getStation();

        Subscription sub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(tx.getUser().getId(), tx.getVehicle().getId(),
                        SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        // 3️⃣ Lấy pin mới theo ID cụ thể từ staff
        BatterySerial newBattery = batterySerialRepository.findById(request.getNewBatterySerialId())
                .orElseThrow(() -> new RuntimeException("Battery not found"));

        // 4️⃣ Validate pin mới
        if (newBattery.getStatus() != BatteryStatus.AVAILABLE) {
            throw new RuntimeException("Battery is not available for swap");
        }

        if (!newBattery.getStation().getId().equals(station.getId())) {
            throw new RuntimeException("Battery is not at the correct station");
        }

        Double chargePercent = Optional.ofNullable(newBattery.getChargePercent()).orElse(0.0);
        if (chargePercent < 95) {
            throw new RuntimeException("Battery charge is below 95%");
        }

        // 🆕 Validate SoH range theo subscription plan
        Double batterySoH = Optional.ofNullable(newBattery.getStateOfHealth()).orElse(100.0);
        Double minSoH = sub.getPlan().getMinSoH();
        Double maxSoH = sub.getPlan().getMaxSoH();

        if (minSoH != null && maxSoH != null) {
            if (batterySoH < minSoH || batterySoH > maxSoH) {
                throw new RuntimeException(String.format(
                        "Battery SoH %.1f%% is not allowed for your plan (allowed: %.1f%% - %.1f%%)",
                        batterySoH, minSoH, maxSoH));
            }
            log.info("SoH VALIDATION PASSED | battery={} | SoH={}% | plan range=[{}%, {}%]",
                    newBattery.getSerialNumber(), batterySoH, minSoH, maxSoH);
        }

        // 5️⃣ Tính toán degradation (di chuyển từ processSwap)
        double designCapacityWh = oldBattery.getBattery().getDesignCapacity();
        double startPercent = Optional.ofNullable(tx.getStartPercent()).orElse(100.0);
        double endPercent = request.getEndPercent(); // Từ staff input
        double depth = Math.max(0, startPercent - endPercent);
        double energyUsedWh = (depth / 100.0) * designCapacityWh;
        double energyUsedKWh = energyUsedWh / 1000.0;
        double cycleUsed = depth / 100.0;
        double degradation = cycleUsed * 0.75; // Degradation cơ bản
        double oldSoH = Optional.ofNullable(oldBattery.getStateOfHealth()).orElse(100.0);
        double newSoH = Math.max(0, oldSoH - degradation);

        // Cập nhật SoH và cycle count của pin cũ
        oldBattery.setStateOfHealth(newSoH);
        oldBattery.setTotalCycleCount(
                Optional.ofNullable(oldBattery.getTotalCycleCount()).orElse(0.0) + cycleUsed);

        double efficiencyKmPerKwh = Optional.ofNullable(tx.getVehicle().getEfficiencyKmPerKwh()).orElse(20.0);
        double distanceTraveled = energyUsedKWh * efficiencyKmPerKwh;

        // Cập nhật transaction với thông tin degradation
        tx.setEndPercent(endPercent);
        tx.setEnergyUsed(energyUsedKWh);
        tx.setDistance(distanceTraveled);
        tx.setDepthOfDischarge(depth);
        tx.setDegradationThisSwap(degradation);

        // Cập nhật trạng thái pin
        // Cập nhật pin cũ - trả về trạm
        oldBattery.setStatus(BatteryStatus.AVAILABLE);
        oldBattery.setStation(tx.getStation());
        oldBattery.setVehicle(null);

        // Auto-charge: Pin tự động sạc đầy khi về trạm
        oldBattery.setChargePercent(100.0);

        // Cập nhật SoH và cycle count dựa trên degradation
        oldBattery.setStateOfHealth(newSoH);
        oldBattery.setTotalCycleCount(oldBattery.getTotalCycleCount() + 1);

        // Cập nhật pin mới - gắn vào xe
        newBattery.setStatus(BatteryStatus.IN_USE);
        newBattery.setVehicle(tx.getVehicle());
        newBattery.setStation(null);

        // 🆕 Increment swap count for both batteries
        oldBattery.setSwapCount(Optional.ofNullable(oldBattery.getSwapCount()).orElse(0) + 1);
        newBattery.setSwapCount(Optional.ofNullable(newBattery.getSwapCount()).orElse(0) + 1);

        // Lưu cả 2 pin
        List<BatterySerial> batteries = Arrays.asList(oldBattery, newBattery);
        batterySerialRepository.saveAll(batteries);

        // 📜 Log History
        User staffUser = userRepository.findById(staffId).orElse(null);

        // Log for old battery (returned to station)
        batteryHistoryService.logEvent(
                oldBattery,
                com.evstation.batteryswap.enums.BatteryEventType.SWAPPED,
                "IN_USE (Vehicle " + tx.getVehicle().getId() + ")",
                "AVAILABLE (Station " + tx.getStation().getId() + ")",
                tx.getStation(),
                null,
                staffUser,
                "Swapped out from vehicle " + tx.getVehicle().getVin());

        // Log for new battery (assigned to vehicle)
        batteryHistoryService.logEvent(
                newBattery,
                com.evstation.batteryswap.enums.BatteryEventType.SWAPPED,
                "AVAILABLE (Station " + tx.getStation().getId() + ")",
                "IN_USE (Vehicle " + tx.getVehicle().getId() + ")",
                null,
                tx.getVehicle(),
                staffUser,
                "Swapped into vehicle " + tx.getVehicle().getVin());

        // Billing Logic - Cập nhật subscription usage và tính phí
        PlanType planType = sub.getPlan().getPlanType();
        double cost = 0.0;
        double overage = 0.0;
        PlanTierRate tierRate = null;

        if (planType == PlanType.ENERGY) {
            // Energy-based billing
            double usedBefore = Optional.ofNullable(sub.getEnergyUsedThisMonth()).orElse(0.0);
            double usageThisSwap = energyUsedKWh;
            double totalAfter = usedBefore + usageThisSwap;
            double base = Optional.ofNullable(sub.getPlan().getBaseEnergy()).orElse(0.0);

            if (totalAfter > base) {
                double chargeableStart = Math.max(usedBefore, base);
                double chargeableEnd = totalAfter;

                cost = calculateTieredCost(PlanType.ENERGY, chargeableStart, chargeableEnd);
                overage = chargeableEnd - chargeableStart;

                tierRate = planTierRateRepository.findTierRate(PlanType.ENERGY, totalAfter)
                        .orElse(null);
            }

            sub.setEnergyUsedThisMonth(totalAfter);

            log.info(
                    "ENERGY BILLING | usedBefore={}kWh | thisSwap={}kWh | total={}kWh | base={}kWh | overage={}kWh | cost={}₫",
                    usedBefore, usageThisSwap, totalAfter, base, overage, cost);

        } else {
            // Distance-based billing
            double usedBefore = Optional.ofNullable(sub.getDistanceUsedThisMonth()).orElse(0.0);
            double usageThisSwap = distanceTraveled;
            double totalAfter = usedBefore + usageThisSwap;
            double base = Optional.ofNullable(sub.getPlan().getBaseMileage()).orElse(0.0);

            if (totalAfter > base) {
                double chargeableStart = Math.max(usedBefore, base);
                double chargeableEnd = totalAfter;

                cost = calculateTieredCost(PlanType.DISTANCE, chargeableStart, chargeableEnd);
                overage = chargeableEnd - chargeableStart;

                tierRate = planTierRateRepository.findTierRate(PlanType.DISTANCE, totalAfter)
                        .orElse(null);
            }

            sub.setDistanceUsedThisMonth(totalAfter);

            log.info(
                    "DISTANCE BILLING | usedBefore={}km | thisSwap={}km | total={}km | base={}km | overage={}km | cost={}₫",
                    usedBefore, usageThisSwap, totalAfter, base, overage, cost);
        }

        subscriptionRepository.save(sub);

        // 9️⃣ Tạo invoice nếu vượt base usage
        if (cost > 0 && overage > 0 && tierRate != null) {
            invoiceService.createInvoice(sub, tx, overage, tierRate.getRate(), planType);
            log.info("SWAP OVERAGE INVOICE CREATED | subscription={} | swap={} | overage={} | cost={}₫",
                    sub.getId(), tx.getId(), overage, cost);
        }

        // 🔟 Cập nhật transaction với cost và staff info
        tx.setCost(cost);
        tx.setStatus(SwapTransactionStatus.COMPLETED);

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        tx.setStaff(staff);
        tx.setConfirmedAt(LocalDateTime.now());
        swapTransactionRepository.save(tx);

        log.info(
                "CONFIRM_SWAP | staff={} | txId={} | oldBattery={} (SoH: {}% -> {}%) -> station={} | newBattery={} (SoH: {}%) -> vehicle={}",
                staff.getUsername(), transactionId,
                oldBattery.getSerialNumber(), oldSoH, newSoH, station.getId(),
                newBattery.getSerialNumber(), batterySoH, tx.getVehicle().getId());

        return "Swap transaction " + transactionId + " confirmed successfully.";
    }

    @Override
    public String rejectSwap(Long transactionId, Long staffId) {

        // 🔍 1️⃣ Lấy giao dịch swap
        SwapTransaction tx = swapTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Swap transaction not found"));

        if (tx.getStatus() != SwapTransactionStatus.PENDING_CONFIRM) {
            throw new RuntimeException("This swap has already been processed");
        }

        BatterySerial oldBattery = tx.getBatterySerial();

        // 🔁 2️⃣ Hoàn lại pin cũ cho xe
        oldBattery.setStatus(BatteryStatus.IN_USE);
        oldBattery.setVehicle(tx.getVehicle());
        oldBattery.setStation(null);
        batterySerialRepository.save(oldBattery);

        // 🗑️ 3️⃣ Tìm và reset pin mới (PENDING_IN) trong trạm
        Station station = tx.getStation();
        batterySerialRepository.findFirstByStationIdAndStatus(station.getId(), BatteryStatus.PENDING_IN)
                .ifPresent(b -> {
                    b.setStatus(BatteryStatus.AVAILABLE);
                    batterySerialRepository.save(b);
                });

        // 4️⃣ Cập nhật transaction
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        tx.setStatus(SwapTransactionStatus.REJECTED);
        tx.setStaff(staff);
        tx.setConfirmedAt(LocalDateTime.now());
        swapTransactionRepository.save(tx);

        log.warn("REJECT_SWAP | staff={} | txId={} | oldBattery={} | restored to vehicle={}",
                staff.getUsername(), transactionId,
                oldBattery.getSerialNumber(), tx.getVehicle().getId());

        return "Swap transaction " + transactionId + " rejected.";
    }

    /**
     * Tính tiền theo bậc thang (progressive tier pricing)
     * Ví dụ DISTANCE:
     * - Tier 1: 100-200km = 5,000₫/km
     * - Tier 2: 200-300km = 4,000₫/km
     * - Tier 3: 300+km = 3,000₫/km
     * 
     * Nếu đi từ 150km → 250km (100km):
     * - 50km trong tier 1 (150-200): 50 × 5,000₫ = 250,000₫
     * - 50km trong tier 2 (200-250): 50 × 4,000₫ = 200,000₫
     * - Tổng: 450,000₫
     * 
     * @param planType   DISTANCE hoặc ENERGY
     * @param rangeStart Điểm bắt đầu tính phí (VD: 150km)
     * @param rangeEnd   Điểm kết thúc (VD: 250km)
     * @return Tổng chi phí theo bậc thang
     */
    private double calculateTieredCost(PlanType planType, double rangeStart, double rangeEnd) {
        List<PlanTierRate> tiers = planTierRateRepository.findByPlanTypeOrderByMinValueAsc(planType);

        if (tiers.isEmpty()) {
            log.warn("No tier rates found for planType={}", planType);
            return 0.0;
        }

        double totalCost = 0.0;
        double currentPosition = rangeStart;

        StringBuilder costBreakdown = new StringBuilder();
        costBreakdown.append(String.format("Tier breakdown [%.2f → %.2f]: ", rangeStart, rangeEnd));

        for (PlanTierRate tier : tiers) {
            double tierMax = Optional.ofNullable(tier.getMaxValue()).orElse(Double.MAX_VALUE);
            if (currentPosition >= tierMax) {
                continue;
            }

            if (rangeEnd <= tier.getMinValue()) {
                break;
            }

            double chargeableStart = Math.max(currentPosition, tier.getMinValue());
            double chargeableEnd = Math.min(rangeEnd, tierMax);
            double chargeableAmount = chargeableEnd - chargeableStart;

            if (chargeableAmount > 0) {
                double tierCost = chargeableAmount * tier.getRate();
                totalCost += tierCost;

                costBreakdown.append(String.format("Tier[%.0f-%.0f]: %.2f × %.0f₫ = %.0f₫; ",
                        tier.getMinValue(), tierMax, chargeableAmount, tier.getRate(), tierCost));

                currentPosition = chargeableEnd;
            }

            if (currentPosition >= rangeEnd) {
                break;
            }
        }

        log.info("{} | Total: {}₫", costBreakdown.toString(), totalCost);
        return totalCost;
    }
}
