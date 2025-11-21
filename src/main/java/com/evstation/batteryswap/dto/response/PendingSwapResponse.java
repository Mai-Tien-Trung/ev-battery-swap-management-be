package com.evstation.batteryswap.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingSwapResponse {
    private Long id;
    private String username;
    private Long vehicleId;
    private String stationName;
    private String batterySerialNumber;
    private String status;
    private String timestamp;
}
