package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.LinkVehicleRequest;
import com.evstation.batteryswap.dto.response.*;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.LinkVehicleService;
import com.evstation.batteryswap.utils.BatterySerialUtil;
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
    private BatterySerialRepository batterySerialRepository;

    @Autowired
    private SwapTransactionRepository swapTransactionRepository;

    @Override
    public LinkVehicleResponse linkVehicle(Long userId, LinkVehicleRequest request) {
        // 1️⃣ Lấy dữ liệu người dùng và xe
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

        // 3️⃣ Kiểm tra subscription đang hoạt động
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

        // 5️⃣ Sinh pin thật dựa trên số lượng trong gói
        List<BatterySerial> batterySerials = new ArrayList<>();

        // Lấy model pin mặc định (ví dụ id = 1)
        Battery batteryModel = batteryRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Battery model not found"));

        for (int i = 0; i < plan.getMaxBatteries(); i++) {
            BatterySerial serial = new BatterySerial();
            serial.setSerialNumber(BatterySerialUtil.generateSerialNumber());
            serial.setSwapCount(0);
            serial.setStatus(BatteryStatus.IN_USE);
            serial.setStation(null); // đang gắn cho user
            serial.setBattery(batteryModel);
            serial.setInitialCapacity(batteryModel.getDesignCapacity());
            serial.setCurrentCapacity(batteryModel.getDesignCapacity());
            serial.setStateOfHealth(100.0);
            batterySerials.add(serial);
        }
        batterySerialRepository.saveAll(batterySerials);

        // 6️⃣ Ghi log phát pin ban đầu (có cả thông tin hao mòn mặc định)
        List<SwapTransaction> logs = new ArrayList<>();
        for (BatterySerial b : batterySerials) {
            SwapTransaction log = new SwapTransaction();
            log.setUser(user);
            log.setVehicle(vehicle);
            log.setBatterySerial(b); // ⚡ GẮN PIN THẬT VÀO LOG
            log.setStation(null);    // phát lúc đăng ký, chưa ở trạm
            log.setTimestamp(LocalDateTime.now());

            // Mặc định khi phát pin lần đầu
            log.setStartPercent(100.0);
            log.setEndPercent(100.0);
            log.setDepthOfDischarge(0.0);
            log.setDegradationThisSwap(0.0);

            logs.add(log);
        }
        swapTransactionRepository.saveAll(logs);

        // 7️⃣ Chuẩn bị response
        VehicleSummaryResponse vehicleRes = new VehicleSummaryResponse(
                vehicle.getId(),
                vehicle.getVin(),
                vehicle.getModel()
        );

        SubscriptionResponse subRes = new SubscriptionResponse(
                subscription.getId(),
                plan.getName(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate()
        );

        List<BatterySummaryResponse> batteryRes = batterySerials.stream()
                .map(b -> new BatterySummaryResponse(
                        b.getId(),
                        b.getSerialNumber(),
                        b.getStatus().name()
                ))
                .toList();

        return new LinkVehicleResponse(
                "Vehicle linked and subscription created successfully. " +
                        plan.getMaxBatteries() + " new batteries assigned.",
                vehicleRes,
                subRes,
                batteryRes
        );
    }
}
