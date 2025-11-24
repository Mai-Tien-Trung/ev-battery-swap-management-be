package com.evstation.batteryswap.dto.response;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingSwapResponse {
    private Long id;
    private String username;
    private Long vehicleId;
    private String stationName;
    private String batterySerialNumber; // Pin cũ đang được trả về

    // Thông tin chi tiết pin cũ
    private String oldBatterySerialNumber;
    private Double oldBatteryChargePercent;
    private Double oldBatterySoH;

    // Danh sách pin available tại trạm (đã filter theo SoH range của plan)
    private List<AvailableBatteryInfo> availableBatteries;

    private String status;
    private String timestamp;
}
