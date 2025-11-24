package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffUpdateBatterySoHResponse {
    private Long batteryId;
    private String serialNumber;
    private Double oldSoH;
    private Double newSoH;
    private Long stationId;
    private String stationName;
    private String message;
}
