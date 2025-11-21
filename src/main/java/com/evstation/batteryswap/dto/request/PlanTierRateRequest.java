package com.evstation.batteryswap.dto.request;

import com.evstation.batteryswap.enums.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanTierRateRequest {
    private Long id;
    private Long planId;          // id gói
    private PlanType planType;    // DISTANCE hoặc ENERGY
    private Double minValue;
    private Double maxValue;
    private Double rate;
    private String note;
}
