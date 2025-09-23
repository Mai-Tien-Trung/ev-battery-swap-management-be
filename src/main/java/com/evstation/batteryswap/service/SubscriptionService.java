package com.evstation.batteryswap.service;

import com.evstation.batteryswap.entity.Subscription;

public interface SubscriptionService {
    Subscription changePlan(Long userId, Long vehicleId, Long newPlanId);
}

