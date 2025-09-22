package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.SubscriptionStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SubscriptionResponse {
    private Long id;
    private String planName;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
}
