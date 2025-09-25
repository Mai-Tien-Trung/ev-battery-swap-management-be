package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private String planName;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
}
