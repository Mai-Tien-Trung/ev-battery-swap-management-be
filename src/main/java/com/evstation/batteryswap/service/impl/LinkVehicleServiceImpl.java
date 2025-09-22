package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.LinkVehicleRequest;
import com.evstation.batteryswap.dto.response.LinkVehicleResponse;
import com.evstation.batteryswap.dto.response.SubscriptionResponse;
import com.evstation.batteryswap.dto.response.VehicleResponse;
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

        if (vehicle.getUser() != null) {
            throw new RuntimeException("Vehicle already assigned to another user");
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        // Gán xe cho user
        vehicle.setUser(user);
        vehicleRepository.save(vehicle);

        // Tạo subscription
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setVehicle(vehicle);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(plan.getDurationDays()));
        subscriptionRepository.save(subscription);

        // Map response
        VehicleResponse vehicleRes = new VehicleResponse();
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
