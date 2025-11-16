package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.SwapTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.BigDecimal;

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

        // 1️⃣ Lấy user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2️⃣ Lấy xe
        Vehicle vehicle = vehicleRepository.findById(req.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        if (!user.getVehicles().contains(vehicle))
            throw new RuntimeException("Vehicle does not belong to this user");

        // 3️⃣ Kiểm tra subscription ACTIVE
        Subscription sub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(user.getId(), vehicle.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription for this vehicle"));
        PlanType planType = sub.getPlan().getPlanType();

        // 4️⃣ Lấy pin cũ
        BatterySerial oldBattery = batterySerialRepository.findById(req.getBatterySerialId())
                .orElseThrow(() -> new RuntimeException("Old battery not found"));
        if (oldBattery.getVehicle() == null || !oldBattery.getVehicle().getId().equals(vehicle.getId()))
            throw new RuntimeException("This battery does not belong to the selected vehicle");
        if (oldBattery.getStatus() != BatteryStatus.IN_USE)
            throw new RuntimeException("This battery is not currently in use");

        // 5️⃣ Lấy trạm
        Station station = stationRepository.findById(req.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        // 6️⃣ Lấy pin mới ngẫu nhiên trong trạm
        BatterySerial newBattery = batterySerialRepository
                .findRandomAvailableBatteryAtStation(station.getId())
                .orElseThrow(() -> new RuntimeException("No available battery at this station"));

        double newBatteryPercent = Optional.ofNullable(newBattery.getChargePercent()).orElse(100.0);
        if (newBatteryPercent < 95)
            throw new RuntimeException("No fully charged battery available at this station");

        // 7️⃣ Tính toán năng lượng hao mòn pin cũ
        double designCapacityWh = oldBattery.getBattery().getDesignCapacity();
        double startPercent = Optional.ofNullable(oldBattery.getChargePercent()).orElse(100.0);
        double endPercent = req.getEndPercent();
        double depth = Math.max(0, startPercent - endPercent);
        double energyUsedWh = (depth / 100.0) * designCapacityWh;
        double energyUsedKWh = energyUsedWh / 1000.0;
        double cycleUsed = depth / 100.0;
        double degradation = cycleUsed * 0.75;
        double oldSoH = Optional.ofNullable(oldBattery.getStateOfHealth()).orElse(100.0);
        double newSoH = Math.max(0, oldSoH - degradation);
        oldBattery.setStateOfHealth(newSoH);
        oldBattery.setTotalCycleCount(
                Optional.ofNullable(oldBattery.getTotalCycleCount()).orElse(0.0) + cycleUsed
        );

        double efficiencyKmPerKwh = Optional.ofNullable(vehicle.getEfficiencyKmPerKwh()).orElse(20.0);
        double distanceTraveled = energyUsedKWh * efficiencyKmPerKwh;

        // 8️⃣ Đặt trạng thái tạm (pending)
        oldBattery.setStatus(BatteryStatus.PENDING_OUT);
        newBattery.setStatus(BatteryStatus.PENDING_IN);
        batterySerialRepository.saveAll(java.util.List.of(oldBattery, newBattery));

        // 9️⃣ Lưu transaction (chưa đổi pin thực tế)
        SwapTransaction tx = SwapTransaction.builder()
                .user(user)
                .vehicle(vehicle)
                .batterySerial(oldBattery)
                .station(station)
                .energyUsed(energyUsedKWh)
                .distance(distanceTraveled)
                .cost(0.0)
                .startPercent(startPercent)
                .endPercent(endPercent)
                .depthOfDischarge(depth)
                .degradationThisSwap(degradation)
                .timestamp(LocalDateTime.now())
                .status(SwapTransactionStatus.PENDING_CONFIRM)
                .build();

        swapTransactionRepository.save(tx);

        // 🔟 Ghi log & trả response
        log.info("SWAP REQUEST | user={} | vehicle={} | oldBattery={} | newBattery={} | status=PENDING_CONFIRM",
                user.getUsername(), vehicle.getId(), oldBattery.getSerialNumber(), newBattery.getSerialNumber());

        return SwapResponse.builder()
                .message("Swap request created. Waiting for staff confirmation at " + station.getName())
                .oldSerialNumber(oldBattery.getSerialNumber())
                .newSerialNumber(newBattery.getSerialNumber())
                .oldSoH(oldSoH)
                .newSoH(newSoH)
                .depthOfDischarge(depth)
                .energyUsed(energyUsedKWh)
                .distanceUsed(distanceTraveled)
                .status(SwapTransactionStatus.PENDING_CONFIRM.name())
                .build();
    }
    @Override
    public List<Map<String, Object>> getMostFrequentSwapHour() {
        List<Object[]> results = swapTransactionRepository.findMostFrequentSwapHour();

        return results.stream().map(arr -> {
            Map<String, Object> map = new HashMap<>();

            //  sửa ở đây:
            BigDecimal hourBigDecimal = (BigDecimal) arr[0];
            Long count = (Long) arr[1];

            map.put("hour", hourBigDecimal.intValue()); // Giờ
            map.put("count", count);
            return map;
        }).collect(Collectors.toList());
    }
}
