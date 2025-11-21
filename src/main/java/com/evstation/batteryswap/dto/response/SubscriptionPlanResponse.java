package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.PlanStatus;
import com.evstation.batteryswap.enums.PlanType;
import lombok.Data;

@Data
public class SubscriptionPlanResponse {
    private Long id;
    private String name;
    private Double price;
    private Integer durationDays;
    private Integer maxBatteries;
    private Double baseMileage;
    private Double baseEnergy;
    private PlanType planType;
    private PlanStatus status;
}
