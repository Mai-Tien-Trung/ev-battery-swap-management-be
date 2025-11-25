package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.BatteryEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatteryHistoryResponse {
    private Long id;
    private BatteryEventType eventType;
    private String oldValue;
    private String newValue;
    private Long stationId;
    private String stationName;
    private Long vehicleId;
    private String vehicleVin;
    private Long performedByUserId;
    private String performedByUsername;
    private String notes;
    private Double soh; // State of Health (%) at the time of the event
    private LocalDateTime createdAt;
}
