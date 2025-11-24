package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.BatteryStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapResponse {
    private String message;
    private String oldSerialNumber;
    private String newSerialNumber;
    private double oldSoH;
    private double newSoH;
    private double depthOfDischarge;
    private double degradationThisSwap;
    private double totalCycleCount;
    private double energyUsed;
    private double distanceUsed;
    private double cost;
    private Double oldBatteryChargedPercent;
    private String status;
    private LocalDateTime requestedAt; // Thời gian user gửi yêu cầu swap
}