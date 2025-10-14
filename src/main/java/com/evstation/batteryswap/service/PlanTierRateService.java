package com.evstation.batteryswap.service;


import com.evstation.batteryswap.dto.request.PlanTierRateRequest;
import com.evstation.batteryswap.dto.response.PlanTierRateResponse;
import com.evstation.batteryswap.enums.PlanType;

import java.util.List;

public interface PlanTierRateService {
    List<PlanTierRateResponse> getAll();
    List<PlanTierRateResponse> getByPlanType(PlanType planType);
    PlanTierRateResponse create(PlanTierRateRequest req);
    PlanTierRateResponse update(Long id, PlanTierRateRequest req);
    void delete(Long id);
}
