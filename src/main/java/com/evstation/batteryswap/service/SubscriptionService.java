package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.response.PlanChangeResponse;
import com.evstation.batteryswap.dto.response.SubscriptionDetailResponse;
import com.evstation.batteryswap.entity.Subscription;

import java.util.List;

public interface SubscriptionService {
    PlanChangeResponse changePlan(Long userId, Long vehicleId, Long newPlanId);
    void autoRenewSubscriptions();
    
    /**
     * Thực hiện gia hạn subscription sau khi đã thanh toán invoice
     * @param subscriptionId ID của subscription cần renew
     * @return Subscription mới đã được tạo
     */
    Subscription completeRenewal(Long subscriptionId);
    
    /**
     * Activate subscription PENDING sau khi thanh toán initial invoice
     * @param subscriptionId ID của subscription cần activate
     * @return Subscription đã được activate
     */
    Subscription activateSubscription(Long subscriptionId);

    /**
     * Activate plan change sau khi thanh toán plan change invoice
     * @param newSubscriptionId ID của subscription mới (PENDING)
     * @return Subscription mới đã được activate
     */
    Subscription activatePlanChange(Long newSubscriptionId);
    
    SubscriptionDetailResponse getSubscriptionDetail(Long userId, Long vehicleId);
    List<SubscriptionDetailResponse> getAllActiveSubscriptions(Long userId);

}

