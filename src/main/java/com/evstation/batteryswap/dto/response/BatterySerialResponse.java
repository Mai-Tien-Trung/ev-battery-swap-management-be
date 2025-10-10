package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.BatteryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatterySerialResponse {
    private Long id;
    private String serialNumber;
    private BatteryStatus status;
    private String stationName;
    private String batteryName;
    private Double currentCapacity;
    private Double stateOfHealth;
}
