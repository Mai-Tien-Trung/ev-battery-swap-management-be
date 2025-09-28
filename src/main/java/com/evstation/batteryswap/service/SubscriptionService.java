package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.response.SubscriptionDetailResponse;
import com.evstation.batteryswap.entity.Subscription;

import java.util.List;

public interface SubscriptionService {
    Subscription changePlan(Long userId, Long vehicleId, Long newPlanId);
    void autoRenewSubscriptions();
    SubscriptionDetailResponse getSubscriptionDetail(Long userId, Long vehicleId);
    List<SubscriptionDetailResponse> getAllActiveSubscriptions(Long userId);

}

