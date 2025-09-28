package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionDetailResponse {
    private String vehicle;
    private String currentPlan;
    private LocalDate startDate;
    private LocalDate endDate;
    private String nextPlan;
}

