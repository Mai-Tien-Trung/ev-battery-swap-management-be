package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.InvoiceService;
import com.evstation.batteryswap.service.SwapTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SwapTransactionServiceImpl implements SwapTransactionService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final BatterySerialRepository batterySerialRepository;
    private final StationRepository stationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanTierRateRepository planTierRateRepository;
    private final SwapTransactionRepository swapTransactionRepository;
    private final InvoiceService invoiceService;

    @Override
    public SwapResponse processSwap(String username, SwapRequest req) {

        // 1 Lấy user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2 Xác định xe
        Vehicle vehicle = vehicleRepository.findById(req.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        if (!user.getVehicles().contains(vehicle))
            throw new RuntimeException("Vehicle does not belong to this user");

        // 3 Lấy subscription ACTIVE
        Subscription sub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(user.getId(), vehicle.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription for this vehicle"));

        PlanType planType = sub.getPlan().getPlanType();

        // 4 Xác định pin đang dùng (người dùng chọn)
        BatterySerial oldBattery = batterySerialRepository.findById(req.getBatterySerialId())
                .orElseThrow(() -> new RuntimeException("Battery not found"));
        if (oldBattery.getVehicle() == null || !oldBattery.getVehicle().getId().equals(vehicle.getId()))
            throw new RuntimeException("This battery does not belong to the selected vehicle");

        if (oldBattery.getStatus() != BatteryStatus.IN_USE)
            throw new RuntimeException("This battery is not currently in use");

        // 5Xác định trạm swap
        Station station = stationRepository.findById(req.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        // ⚡ TÍNH TOÁN NĂNG LƯỢNG & HAO MÒN
        double designCapacityWh = oldBattery.getBattery().getDesignCapacity(); // Wh

        // Lấy phần trăm pin thực tế khi người dùng bắt đầu sử dụng pin
        double startPercent = Optional.ofNullable(oldBattery.getChargePercent()).orElse(100.0);
        double endPercent = req.getEndPercent();

            // Độ sâu xả (Depth of Discharge)
        double depth = startPercent - endPercent;
        if (depth < 0) depth = 0;

        // Năng lượng sử dụng
        double energyUsedWh = (depth / 100.0) * designCapacityWh;
        double energyUsedKWh = energyUsedWh / 1000.0;


        // Equivalent Full Cycle (EFC)
        double cycleUsed = depth / 100.0;
        // Hao mòn pin: mỗi cycle giảm ~0.75% SoH
        double degradation = cycleUsed * 0.75;
        double oldSoH = Optional.ofNullable(oldBattery.getStateOfHealth()).orElse(100.0);
        double newSoH = Math.max(0, oldSoH - degradation);

        oldBattery.setStateOfHealth(newSoH);
        oldBattery.setTotalCycleCount(
                Optional.ofNullable(oldBattery.getTotalCycleCount()).orElse(0.0) + cycleUsed
        );

        // Quy đổi ra quãng đường (theo hiệu suất xe)
        double efficiencyKmPerKwh = Optional.ofNullable(vehicle.getEfficiencyKmPerKwh()).orElse(8.0);
        double distanceTraveled = energyUsedKWh * efficiencyKmPerKwh;

        // 6Cập nhật pin cũ về trạm
        oldBattery.setVehicle(null);
        oldBattery.setStation(station);
        double randomChargedPercent = 0.0;

        if (newSoH >= 80) {
             randomChargedPercent = 95 + new Random().nextDouble() * 5; // 95–100%
            randomChargedPercent = Math.round(randomChargedPercent * 10.0) / 10.0;
            oldBattery.setChargePercent(randomChargedPercent);
            oldBattery.setStatus(BatteryStatus.AVAILABLE);
        } else {
            randomChargedPercent = req.getEndPercent();
            oldBattery.setChargePercent(req.getEndPercent()); // nếu SoH < 80, giữ nguyên %
            oldBattery.setStatus(BatteryStatus.MAINTENANCE);
        }

        batterySerialRepository.save(oldBattery);

        //  Cấp pin mới cho xe
        BatterySerial newBattery = batterySerialRepository
                .findRandomAvailableBatteryAtStation(station.getId())
                .orElseThrow(() -> new RuntimeException("No available battery at this station"));
        newBattery.setStatus(BatteryStatus.IN_USE);
        newBattery.setVehicle(vehicle);
        newBattery.setStation(null);
        batterySerialRepository.save(newBattery);

        //  Cập nhật subscription usage
        double cost = 0.0;
        double overage = 0.0;
        PlanTierRate tierRate = null;
        
        if (planType == PlanType.ENERGY) {
            double usedBefore = Optional.ofNullable(sub.getEnergyUsedThisMonth()).orElse(0.0);
            double totalAfter = usedBefore + energyUsedKWh;
            double base = Optional.ofNullable(sub.getPlan().getBaseEnergy()).orElse(0.0);

            if (totalAfter > base) {
                overage = totalAfter - base;
                tierRate = planTierRateRepository.findTierRate(PlanType.ENERGY, totalAfter)
                        .orElseThrow(() -> new RuntimeException("No ENERGY tier found"));
                cost = overage * tierRate.getRate();
            }

            sub.setEnergyUsedThisMonth(totalAfter);
        } else {
            double usedBefore = Optional.ofNullable(sub.getDistanceUsedThisMonth()).orElse(0.0);
            double totalAfter = usedBefore + distanceTraveled;
            double base = Optional.ofNullable(sub.getPlan().getBaseMileage()).orElse(0.0);

            if (totalAfter > base) {
                overage = totalAfter - base;
                tierRate = planTierRateRepository.findTierRate(PlanType.DISTANCE, totalAfter)
                        .orElseThrow(() -> new RuntimeException("No DISTANCE tier found"));
                cost = overage * tierRate.getRate();
            }

            sub.setDistanceUsedThisMonth(totalAfter);
        }

        subscriptionRepository.save(sub);

        // 9 Lưu transaction
        SwapTransaction tx = SwapTransaction.builder()
                .user(user)
                .vehicle(vehicle)
                .batterySerial(oldBattery)
                .station(station)
                .energyUsed(energyUsedKWh)
                .distance(distanceTraveled)
                .cost(cost)
                .startPercent(startPercent)
                .endPercent(req.getEndPercent())
                .depthOfDischarge(depth)
                .degradationThisSwap(degradation * 100) // hiển thị %
                .timestamp(LocalDateTime.now())
                .build();

        swapTransactionRepository.save(tx);

        log.info("SWAP | user={} | planType={} | energyUsed={}kWh | distance={}km | cost={}₫ | ΔSoH={}%",
                user.getUsername(), planType, energyUsedKWh, distanceTraveled, cost, degradation * 100);

        // 10 Tạo invoice nếu vượt base usage
        if (cost > 0 && overage > 0 && tierRate != null) {
            invoiceService.createInvoice(sub, tx, overage, tierRate.getRate(), planType);
        }

        // Trả response
        return SwapResponse.builder()
                .message("Swap completed successfully at station " + station.getName())
                .oldSerialNumber(oldBattery.getSerialNumber())
                .newSerialNumber(newBattery.getSerialNumber())
                .oldSoH(oldSoH)
                .newSoH(newSoH)
                .depthOfDischarge(depth)
                .degradationThisSwap(degradation)
                .totalCycleCount(oldBattery.getTotalCycleCount())
                .energyUsed(energyUsedKWh)
                .distanceUsed(distanceTraveled)
                .cost(cost)
                .status(oldBattery.getStatus())
                .oldBatteryChargedPercent(randomChargedPercent)
                .build();
    }
}
