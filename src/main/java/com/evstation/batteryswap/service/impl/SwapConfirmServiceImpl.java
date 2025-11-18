package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.PlanType;
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

    @Override
    public String confirmSwap(Long transactionId, Long staffId) {

        // 🔍 1️⃣ Lấy giao dịch swap
        SwapTransaction tx = swapTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Swap transaction not found"));

        if (tx.getStatus() != SwapTransactionStatus.PENDING_CONFIRM) {
            throw new RuntimeException("This swap has already been processed");
        }

        // 2️⃣ Lấy pin cũ và trạm liên quan
        BatterySerial oldBattery = tx.getBatterySerial();
        Station station = tx.getStation();

        // 3️⃣ Tìm pin mới đang PENDING_IN tại cùng trạm
        BatterySerial newBattery = batterySerialRepository
                .findFirstByStationIdAndStatus(station.getId(), BatteryStatus.PENDING_IN)
                .orElseThrow(() -> new RuntimeException("No pending battery found for this swap"));

        // 4️⃣ Cập nhật trạng thái pin
        // 🔹 Pin cũ -> trả về trạm, sẵn sàng dùng
        oldBattery.setVehicle(null);
        oldBattery.setStation(station);
        oldBattery.setStatus(BatteryStatus.AVAILABLE);

        // 🔹 Pin mới -> gắn vào xe
        newBattery.setVehicle(tx.getVehicle());
        newBattery.setStation(null);
        newBattery.setStatus(BatteryStatus.IN_USE);

        batterySerialRepository.saveAll(List.of(oldBattery, newBattery));

        // 5️⃣ Billing Logic - Cập nhật subscription usage và tính phí
        Subscription sub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(tx.getUser().getId(), tx.getVehicle().getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        PlanType planType = sub.getPlan().getPlanType();
        double cost = 0.0;
        double overage = 0.0;
        PlanTierRate tierRate = null;

        if (planType == PlanType.ENERGY) {
            // Energy-based billing
            double usedBefore = Optional.ofNullable(sub.getEnergyUsedThisMonth()).orElse(0.0);
            double usageThisSwap = tx.getEnergyUsed();
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
            
            log.info("ENERGY BILLING | usedBefore={}kWh | thisSwap={}kWh | total={}kWh | base={}kWh | overage={}kWh | cost={}₫",
                    usedBefore, usageThisSwap, totalAfter, base, overage, cost);
                    
        } else {
            // Distance-based billing
            double usedBefore = Optional.ofNullable(sub.getDistanceUsedThisMonth()).orElse(0.0);
            double usageThisSwap = tx.getDistance();
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
            
            log.info("DISTANCE BILLING | usedBefore={}km | thisSwap={}km | total={}km | base={}km | overage={}km | cost={}₫",
                    usedBefore, usageThisSwap, totalAfter, base, overage, cost);
        }

        subscriptionRepository.save(sub);

        // 6️⃣ Tạo invoice nếu vượt base usage
        if (cost > 0 && overage > 0 && tierRate != null) {
            invoiceService.createInvoice(sub, tx, overage, tierRate.getRate(), planType);
            log.info("SWAP OVERAGE INVOICE CREATED | subscription={} | swap={} | overage={} | cost={}₫",
                    sub.getId(), tx.getId(), overage, cost);
        }

        // 7️⃣ Cập nhật transaction với cost
        tx.setCost(cost);
        tx.setStatus(SwapTransactionStatus.COMPLETED);
        
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        tx.setStaff(staff);
        tx.setConfirmedAt(LocalDateTime.now());
        swapTransactionRepository.save(tx);

        log.info("CONFIRM_SWAP | staff={} | txId={} | oldBattery={} -> station={} | newBattery={} -> vehicle={}",
                staff.getUsername(), transactionId,
                oldBattery.getSerialNumber(), station.getId(),
                newBattery.getSerialNumber(), tx.getVehicle().getId());

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
     * @param planType DISTANCE hoặc ENERGY
     * @param rangeStart Điểm bắt đầu tính phí (VD: 150km)
     * @param rangeEnd Điểm kết thúc (VD: 250km)
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
