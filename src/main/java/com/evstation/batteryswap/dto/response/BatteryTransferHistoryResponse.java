package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatteryTransferHistoryResponse {
    private Long batteryId;
    private String batterySerialNumber;
    private String batteryModelName;
    private Double stateOfHealth;
    private Long fromStationId;
    private String fromStationName;
    private Long toStationId;
    private String toStationName;
    private String notes;
    private Long performedByUserId;
    private String performedByUsername;
    private LocalDateTime transferredAt;
}
