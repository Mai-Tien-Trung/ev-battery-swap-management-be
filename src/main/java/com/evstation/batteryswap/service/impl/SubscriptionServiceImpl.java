package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.entity.SubscriptionPlan;
import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.entity.Vehicle;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.SubscriptionPlanRepository;
import com.evstation.batteryswap.repository.SubscriptionRepository;
import com.evstation.batteryswap.repository.UserRepository;
import com.evstation.batteryswap.repository.VehicleRepository;
import com.evstation.batteryswap.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public Subscription changePlan(Long userId, Long vehicleId, Long newPlanId) {
        // 1. Tìm subscription ACTIVE hiện tại
        Subscription currentSub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(userId, vehicleId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy subscription ACTIVE cho vehicleId=" + vehicleId));

        // 2. Lấy gói mới
        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy gói với id=" + newPlanId));

        // Nếu user đổi sang cùng 1 gói đang dùng -> báo lỗi
        if (currentSub.getPlan().getId().equals(newPlanId)) {
            throw new IllegalStateException("Xe này đã dùng gói " + newPlan.getName() + " rồi");
        }

        // 3. Đổi trạng thái gói cũ -> CANCELLED
        currentSub.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(currentSub);

        // 4. Load user & vehicle entity
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle không tồn tại"));

        // 5. Tạo subscription mới
        Subscription newSub = new Subscription();
        newSub.setUser(user);
        newSub.setVehicle(vehicle);
        newSub.setPlan(newPlan);
        newSub.setStatus(SubscriptionStatus.ACTIVE);
        newSub.setStartDate(LocalDate.now());

        return subscriptionRepository.save(newSub);
    }
}
