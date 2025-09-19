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

    @NotBlank(message = "Serial number không được để trống")
    private String serialNumber;

    @NotNull(message = "Trạng thái pin không được để trống")
    private BatteryStatus status;

    @Min(value = 0, message = "Số lần swap không thể âm")
    private int swapCount;

    @NotNull(message = "Station ID không được để trống")
    private Long stationId;
}
