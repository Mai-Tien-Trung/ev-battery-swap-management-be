package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.LinkVehicleRequest;
import com.evstation.batteryswap.dto.response.LinkVehicleResponse;
import com.evstation.batteryswap.dto.response.SubscriptionResponse;
import com.evstation.batteryswap.dto.response.VehicleResponse;
import com.evstation.batteryswap.dto.response.VehicleSummaryResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.LinkVehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

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

        // Map response
        VehicleSummaryResponse vehicleRes = new VehicleSummaryResponse();
        vehicleRes.setId(vehicle.getId());
        vehicleRes.setVin(vehicle.getVin());
        vehicleRes.setModel(vehicle.getModel());

        SubscriptionResponse subRes = new SubscriptionResponse();
        subRes.setId(subscription.getId());
        subRes.setPlanName(plan.getName());
        subRes.setStatus(subscription.getStatus());
        subRes.setStartDate(subscription.getStartDate());
        subRes.setEndDate(subscription.getEndDate());

        return new LinkVehicleResponse(
                "Vehicle linked and subscription created successfully",
                vehicleRes,
                subRes
        );
    }
}