package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.BatteryStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatteryResponse {
    private Long id;
    private String serialNumber;
    private BatteryStatus status;
    private Long stationId;
    private String stationName;
    private Double stateOfHealth;
    private Integer swapCount;
}
