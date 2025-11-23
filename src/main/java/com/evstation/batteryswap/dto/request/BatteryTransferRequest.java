package com.evstation.batteryswap.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatteryTransferRequest {
    private Long batteryId;
    private Long fromStationId;
    private Long toStationId;
    private String notes; // Optional reason for transfer
}
