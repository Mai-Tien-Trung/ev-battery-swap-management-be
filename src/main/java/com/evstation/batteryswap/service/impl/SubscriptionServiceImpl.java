package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.response.SubscriptionDetailResponse;
import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.entity.SubscriptionPlan;

import com.evstation.batteryswap.entity.Vehicle;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.SubscriptionPlanRepository;
import com.evstation.batteryswap.repository.SubscriptionRepository;

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

    @Override
    @Transactional
    public Subscription changePlan(Long userId, Long vehicleId, Long newPlanId) {
        // 1. Tìm subscription ACTIVE hiện tại
        Subscription currentSub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(userId, vehicleId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy subscription ACTIVE cho xe này"));

        // 2. Nếu user đổi sang cùng gói -> báo lỗi
        if (currentSub.getPlan().getId().equals(newPlanId)) {
            throw new IllegalStateException("Xe này đã dùng gói này rồi");
        }

        // 3. Chỉ set nextPlanId, không cancel, không tạo mới ngay
        currentSub.setNextPlanId(newPlanId);

        return subscriptionRepository.save(currentSub);
    }

    @Override
    @Transactional
    public void autoRenewSubscriptions() {
        LocalDate today = LocalDate.now();

        List<Subscription> expiredSubs =
                subscriptionRepository.findByStatusAndEndDate(SubscriptionStatus.ACTIVE, today);

        for (Subscription sub : expiredSubs) {
            // 1. Lưu lại nextPlanId trước
            Long planId = (sub.getNextPlanId() != null)
                    ? sub.getNextPlanId()
                    : sub.getPlan().getId();

            // 2. Đóng gói cũ
            sub.setStatus(SubscriptionStatus.COMPLETED);
            sub.setNextPlanId(null); // reset sau khi đã lấy ra
            subscriptionRepository.save(sub);

            // 3. Lấy plan mới
            SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan không tồn tại"));

            // 4. Tạo subscription mới
            Subscription newSub = new Subscription();
            newSub.setUser(sub.getUser());
            newSub.setVehicle(sub.getVehicle());
            newSub.setPlan(plan);
            newSub.setStatus(SubscriptionStatus.ACTIVE);
            newSub.setStartDate(sub.getEndDate().plusDays(1));
            newSub.setEndDate(sub.getEndDate().plusDays(plan.getDurationDays()));
            newSub.setNextPlanId(null);

            subscriptionRepository.save(newSub);
        }

    }
    @Override
    public SubscriptionDetailResponse getSubscriptionDetail(Long userId, Long vehicleId) {
        Subscription sub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(userId, vehicleId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        Vehicle vehicle = sub.getVehicle();

        SubscriptionPlan currentPlan = sub.getPlan();
        SubscriptionPlan nextPlan = sub.getNextPlanId() != null
                ? subscriptionPlanRepository.findById(sub.getNextPlanId())
                .orElseThrow(() -> new RuntimeException("Next plan not found"))
                : currentPlan;

        return new SubscriptionDetailResponse(
                vehicle.getModel() + " " + vehicle.getVin(),
                currentPlan.getName(),
                sub.getStartDate(),
                sub.getEndDate(),
                nextPlan.getName()
        );
    }
    @Override
    public List<SubscriptionDetailResponse> getAllActiveSubscriptions(Long userId) {
        List<Subscription> subs = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        return subs.stream().map(sub -> {
            Vehicle vehicle = sub.getVehicle();
            SubscriptionPlan currentPlan = sub.getPlan();
            SubscriptionPlan nextPlan = sub.getNextPlanId() != null
                    ? subscriptionPlanRepository.findById(sub.getNextPlanId())
                    .orElseThrow(() -> new RuntimeException("Next plan not found"))
                    : currentPlan;

            return new SubscriptionDetailResponse(
                    vehicle.getModel().getName() + " " + vehicle.getVin(),
                    currentPlan.getName(),
                    sub.getStartDate(),
                    sub.getEndDate(),
                    nextPlan.getName()
            );
        }).toList();
    }

}
