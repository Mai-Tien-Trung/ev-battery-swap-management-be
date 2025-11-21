package com.evstation.batteryswap.dto.request;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapRequest {
    private Long vehicleId;
    private Long batterySerialId;
    private Long stationId;     // Trạm đang swap
    private double endPercent;
}
