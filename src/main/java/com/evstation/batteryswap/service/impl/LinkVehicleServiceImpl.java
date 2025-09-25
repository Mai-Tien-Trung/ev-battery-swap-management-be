package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.LinkVehicleRequest;
import com.evstation.batteryswap.dto.response.*;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.LinkVehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LinkVehicleServiceImpl implements LinkVehicleService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private BatteryRepository batteryRepository;

    @Autowired
    private SwapTransactionRepository swapTransactionRepository;

    @Override
    public LinkVehicleResponse linkVehicle(Long userId, LinkVehicleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        // Thêm vehicle vào garage của user (user_vehicle)
        if (!user.getVehicles().contains(vehicle)) {
            user.getVehicles().add(vehicle);
            userRepository.save(user);
        }

        // Kiểm tra đã có subscription active cho xe này chưa
        boolean hasActiveSub = subscriptionRepository
                .existsByUserIdAndVehicleIdAndStatus(userId, vehicle.getId(), SubscriptionStatus.ACTIVE);
        if (hasActiveSub) {
            throw new RuntimeException("User already has an active subscription for this vehicle");
        }

        // Tạo subscription mới
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setVehicle(vehicle);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(plan.getDurationDays()));
        subscriptionRepository.save(subscription);

        // 👉 Sinh pin ban đầu theo số lượng trong gói
        List<Battery> batteries = new ArrayList<>();
        for (int i = 0; i < plan.getMaxBatteries(); i++) {
            Battery battery = new Battery();
            battery.setSerialNumber("BAT-" + UUID.randomUUID());
            battery.setSwapCount(0);
            battery.setStatus(BatteryStatus.IN_USE);
            battery.setStation(null); // đang gắn cho user, không ở station
            batteries.add(battery);
        }
        batteryRepository.saveAll(batteries);

        // 👉 Log phát pin ban đầu
        List<SwapTransaction> logs = new ArrayList<>();
        for (Battery b : batteries) {
            SwapTransaction log = new SwapTransaction();
            log.setUser(user);
            log.setVehicle(vehicle);
            log.setOldBattery(null); // dealer cấp pin mới
            log.setNewBattery(b);
            log.setStation(null); // không phát tại station
            log.setTimestamp(LocalDateTime.now());
            logs.add(log);
        }
        swapTransactionRepository.saveAll(logs);

        // Map response
        VehicleSummaryResponse vehicleRes = new VehicleSummaryResponse(
                vehicle.getId(), vehicle.getVin(), vehicle.getModel()
        );

        SubscriptionResponse subRes = new SubscriptionResponse(
                subscription.getId(),
                plan.getName(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate()
        );

        List<BatterySummaryResponse> batteryRes = batteries.stream()
                .map(b -> new BatterySummaryResponse(
                        b.getId(),
                        b.getSerialNumber(),
                        b.getStatus().name()
                ))
                .toList();

        return new LinkVehicleResponse(
                "Vehicle linked and subscription created successfully. Initial batteries assigned.",
                vehicleRes,
                subRes,
                batteryRes
        );
    }
}