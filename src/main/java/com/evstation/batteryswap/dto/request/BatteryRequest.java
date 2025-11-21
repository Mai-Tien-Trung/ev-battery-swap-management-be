package com.evstation.batteryswap.dto.request;

import com.evstation.batteryswap.enums.BatteryStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatteryRequest {

    private String serialNumber;

    @NotNull(message = "Trạng thái pin không được để trống")
    private BatteryStatus status;

    @NotNull(message = "Station ID không được để trống")
    private Long stationId;

    @NotNull(message = "Battery ID (loại pin) không được để trống")
    private Long batteryId;
}
