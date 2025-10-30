package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.response.*;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.UserVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserVehicleServiceImpl implements UserVehicleService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BatterySerialRepository batterySerialRepository;

    @Override
    public List<VehicleWithBatteriesResponse> getUserVehicles(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getVehicles().stream().map(vehicle -> {
            Subscription sub = subscriptionRepository
                    .findTopByUserIdAndVehicleIdOrderByStartDateDesc(userId, vehicle.getId())
                    .orElse(null);

            // lấy pin của xe
            List<BatterySummaryResponse> batteries = batterySerialRepository
                    .findByVehicleId(vehicle.getId())
                    .stream()
                    .map(b -> BatterySummaryResponse.builder()
                            .id(b.getId())
                            .serialNumber(b.getSerialNumber())
                            .status(b.getStatus().name())
                            .build())
                    .toList();

            return VehicleWithBatteriesResponse.builder()
                    .vehicleId(vehicle.getId())
                    .vin(vehicle.getVin())
                    .modelName(vehicle.getModel().getName())
                    .planName(sub != null ? sub.getPlan().getName() : null)
                    .subscriptionStatus(sub != null ? sub.getStatus().name() : "NONE")
                    .batteries(batteries)
                    .build();
        }).toList();
    }
}
