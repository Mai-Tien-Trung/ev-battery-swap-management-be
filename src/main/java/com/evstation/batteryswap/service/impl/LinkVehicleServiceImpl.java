package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.LinkVehicleRequest;
import com.evstation.batteryswap.dto.response.BatterySummaryResponse;
import com.evstation.batteryswap.dto.response.LinkVehicleResponse;
import com.evstation.batteryswap.dto.response.SubscriptionResponse;
import com.evstation.batteryswap.dto.response.VehicleSummaryResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.LinkVehicleService;
import com.evstation.batteryswap.utils.BatterySerialUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkVehicleServiceImpl implements LinkVehicleService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BatteryRepository batteryRepository;
    private final BatterySerialRepository batterySerialRepository;
    private final SwapTransactionRepository swapTransactionRepository;

    @Override
    public LinkVehicleResponse linkVehicle(Long userId, LinkVehicleRequest request) {

        // 1️⃣ Lấy thông tin user, vehicle, plan
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubscriptionPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        // 2️⃣ Gán vehicle cho user (nếu chưa có)
        if (!user.getVehicles().contains(vehicle)) {
            user.getVehicles().add(vehicle);
            userRepository.save(user);
        }

        // 3️⃣ Kiểm tra subscription ACTIVE
        boolean hasActiveSub = subscriptionRepository
                .existsByUserIdAndVehicleIdAndStatus(userId, vehicle.getId(), SubscriptionStatus.ACTIVE);
        if (hasActiveSub) {
            throw new RuntimeException("User already has an active subscription for this vehicle");
        }

        // 4️⃣ Tạo subscription mới
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setVehicle(vehicle);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(plan.getDurationDays()));
        subscriptionRepository.save(subscription);

        // 5️⃣ Lấy model pin mặc định
        Battery batteryModel = batteryRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Battery model not found"));

        // 6️⃣ Sinh pin thật (dựa trên maxBatteries trong gói)
        List<BatterySerial> batterySerials = new ArrayList<>();
        for (int i = 0; i < plan.getMaxBatteries(); i++) {
            BatterySerial serial = BatterySerial.builder()
                    .serialNumber(BatterySerialUtil.generateSerialNumber())
                    .status(BatteryStatus.IN_USE)
                    .battery(batteryModel)
                    .vehicle(vehicle) // ⚡ pin gắn cho xe này
                    .station(null)    // dealer phát, chưa thuộc trạm
                    .initialCapacity(batteryModel.getDesignCapacity())
                    .currentCapacity(batteryModel.getDesignCapacity())
                    .stateOfHealth(100.0)
                    .totalCycleCount(0.0)
                    .build();

            batterySerials.add(serial);
        }
        batterySerialRepository.saveAll(batterySerials);

        // 7️⃣ Ghi log phát pin lần đầu
        List<SwapTransaction> logs = new ArrayList<>();
        for (BatterySerial b : batterySerials) {
            SwapTransaction log = SwapTransaction.builder()
                    .user(user)
                    .vehicle(vehicle)
                    .batterySerial(b)
                    .station(null) // dealer giao, không ở trạm
                    .timestamp(LocalDateTime.now())
                    .startPercent(100.0)
                    .endPercent(100.0)
                    .depthOfDischarge(0.0)
                    .degradationThisSwap(0.0)
                    .energyUsed(0.0)
                    .cost(0.0)
                    .build();

            logs.add(log);
        }
        swapTransactionRepository.saveAll(logs);

        // 8️⃣ Chuẩn bị response
        VehicleSummaryResponse vehicleRes = VehicleSummaryResponse.builder()
                .id(vehicle.getId())
                .vin(vehicle.getVin())
                .model(vehicle.getModel())
                .build();

        SubscriptionResponse subRes = SubscriptionResponse.builder()
                .id(subscription.getId())
                .planName(plan.getName())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .build();

        List<BatterySummaryResponse> batteryRes = batterySerials.stream()
                .map(b -> BatterySummaryResponse.builder()
                        .id(b.getId())
                        .serialNumber(b.getSerialNumber())
                        .status(b.getStatus().name())
                        .build())
                .toList();

        return LinkVehicleResponse.builder()
                .message("Vehicle linked and subscription created successfully. "
                        + plan.getMaxBatteries() + " new batteries assigned.")
                .vehicle(vehicleRes)
                .subscription(subRes)
                .batteries(batteryRes)
                .build();
    }
}
