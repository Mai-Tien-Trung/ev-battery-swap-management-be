package com.evstation.batteryswap.dto.response;


import com.evstation.batteryswap.enums.PlanType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanTierRateResponse {
    private Long id;
    private PlanType planType;
    private Double minValue;
    private Double maxValue;
    private Double rate;
}
