package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.SubscriptionPlanRequest;
import com.evstation.batteryswap.dto.response.SubscriptionPlanResponse;

import java.util.List;

public interface SubscriptionPlanService {
    List<SubscriptionPlanResponse> getAll();
    SubscriptionPlanResponse getById(Long id);
    SubscriptionPlanResponse create(SubscriptionPlanRequest request);
    SubscriptionPlanResponse update(Long id, SubscriptionPlanRequest request);
    void delete(Long id);
}
