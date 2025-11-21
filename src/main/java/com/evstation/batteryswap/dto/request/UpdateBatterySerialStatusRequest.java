package com.evstation.batteryswap.dto.request;

import com.evstation.batteryswap.enums.BatteryStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UpdateBatterySerialStatusRequest {
    private BatteryStatus status;
}
