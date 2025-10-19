package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.SwapTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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
        double depth = 100 - req.getEndPercent(); // % xả
        double energyUsedWh = (depth / 100.0) * designCapacityWh;
        double energyUsedKWh = energyUsedWh / 1000.0;

        // Equivalent Full Cycle (EFC)
        double cycleUsed = energyUsedWh / designCapacityWh;

        // Hao mòn pin: mỗi cycle giảm ~0.75% SoH
        double degradation = cycleUsed * 0.75;
        double oldSoH = oldBattery.getStateOfHealth();
        double newSoH = Math.max(0, oldSoH - degradation);

        oldBattery.setStateOfHealth(newSoH);
        oldBattery.setTotalCycleCount(oldBattery.getTotalCycleCount() + cycleUsed);

        // Quy đổi ra quãng đường (theo hiệu suất xe)
        double efficiencyKmPerKwh = Optional.ofNullable(vehicle.getEfficiencyKmPerKwh()).orElse(8.0);
        double distanceTraveled = energyUsedKWh * efficiencyKmPerKwh;

        // 6Cập nhật pin cũ về trạm
        oldBattery.setVehicle(null);
        oldBattery.setStation(station);
        oldBattery.setStatus(newSoH < 80 ? BatteryStatus.MAINTENANCE : BatteryStatus.AVAILABLE);
        batterySerialRepository.save(oldBattery);

        //  Cấp pin mới cho xe
        BatterySerial newBattery = batterySerialRepository
                .findFirstByStationAndStatus(station, BatteryStatus.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("No available battery at this station"));

        newBattery.setStatus(BatteryStatus.IN_USE);
        newBattery.setVehicle(vehicle);
        newBattery.setStation(null);
        batterySerialRepository.save(newBattery);

        //  Cập nhật subscription usage
        double cost = 0.0;
        if (planType == PlanType.ENERGY) {
            double usedBefore = Optional.ofNullable(sub.getEnergyUsedThisMonth()).orElse(0.0);
            double totalAfter = usedBefore + energyUsedKWh;
            double base = Optional.ofNullable(sub.getPlan().getBaseEnergy()).orElse(0.0);

            if (totalAfter > base) {
                double overage = totalAfter - base;
                PlanTierRate tier = planTierRateRepository.findTierRate(PlanType.ENERGY, totalAfter)
                        .orElseThrow(() -> new RuntimeException("No ENERGY tier found"));
                cost = overage * tier.getRate();
            }

            sub.setEnergyUsedThisMonth(totalAfter);
        } else {
            double usedBefore = Optional.ofNullable(sub.getDistanceUsedThisMonth()).orElse(0.0);
            double totalAfter = usedBefore + distanceTraveled;
            double base = Optional.ofNullable(sub.getPlan().getBaseMileage()).orElse(0.0);

            if (totalAfter > base) {
                double overage = totalAfter - base;
                PlanTierRate tier = planTierRateRepository.findTierRate(PlanType.DISTANCE, totalAfter)
                        .orElseThrow(() -> new RuntimeException("No DISTANCE tier found"));
                cost = overage * tier.getRate();
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
                .startPercent(100.0)
                .endPercent(req.getEndPercent())
                .depthOfDischarge(depth)
                .degradationThisSwap(degradation * 100) // hiển thị %
                .timestamp(LocalDateTime.now())
                .build();

        swapTransactionRepository.save(tx);

        log.info("✅ SWAP | user={} | planType={} | energyUsed={}kWh | distance={}km | cost={}₫ | ΔSoH={}%",
                user.getUsername(), planType, energyUsedKWh, distanceTraveled, cost, degradation * 100);

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
                .build();
    }
}
