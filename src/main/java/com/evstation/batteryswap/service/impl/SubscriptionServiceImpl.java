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
import java.util.List;

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
    @Override
    @Transactional
    public void autoRenewSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> expiredSubs = subscriptionRepository.findByEndDate(today);

        for (Subscription sub : expiredSubs) {
            // 1. đóng gói cũ
            sub.setStatus(SubscriptionStatus.COMPLETED);
            subscriptionRepository.save(sub);

            // 2. chọn gói mới (nextPlanId hoặc giữ nguyên)
            Long planId = (sub.getNextPlanId() != null) ? sub.getNextPlanId() : sub.getPlan().getId();
            SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan không tồn tại"));

            // 3. tạo subscription mới
            Subscription newSub = new Subscription();
            newSub.setUser(sub.getUser());
            newSub.setVehicle(sub.getVehicle());
            newSub.setPlan(plan);
            newSub.setStatus(SubscriptionStatus.ACTIVE);
            newSub.setStartDate(today.plusDays(1));
            newSub.setEndDate(today.plusDays(plan.getDurationDays()));
            newSub.setNextPlanId(null); // reset vì đã đổi gói

            subscriptionRepository.save(newSub);
        }
    }
}
