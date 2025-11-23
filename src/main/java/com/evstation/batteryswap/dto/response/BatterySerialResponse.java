package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.BatteryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatterySerialResponse {
    private Long id;
    private String serialNumber;
    private BatteryStatus status;
    private Double stateOfHealth;
    private Double chargePercent;
    private Double currentCapacity;
    private Double initialCapacity;
    private Double totalCycleCount;
    private Integer swapCount; // Number of successful swaps
    private Long stationId;
    private String stationName;
    private Long batteryModelId;
    private String batteryName;
    private LocalDateTime updatedAt; // Last update time
}
