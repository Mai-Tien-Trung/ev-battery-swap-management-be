package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.PlanStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanResponse {
    private Long id;
    private String name;
    private double price;
    private int durationDays;
    private int swapLimit;
    private double baseMileage;
    private PlanStatus status;
}
