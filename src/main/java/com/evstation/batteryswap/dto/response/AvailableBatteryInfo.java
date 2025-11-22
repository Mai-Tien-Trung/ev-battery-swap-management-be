package com.evstation.batteryswap.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableBatteryInfo {
    private Long id; // Battery serial ID
    private String serialNumber; // Serial number
    private Double chargePercent; // % pin hiện tại
    private Double stateOfHealth; // SoH (State of Health - độ hao mòn, %)
    private Double totalCycleCount; // Tổng số chu kỳ sử dụng
    private String batteryModel; // Tên model pin (để staff biết loại pin)
}
