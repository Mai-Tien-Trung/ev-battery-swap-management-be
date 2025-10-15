package com.evstation.batteryswap.dto.request;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapRequest {
    private Long vehicleId;
    private Long newBatteryId;  // Pin user nhận từ trạm
    private Long stationId;     // Trạm đang swap
    private double endPercent;  // % pin cũ còn lại khi trả
}
