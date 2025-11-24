package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBatterySoHResponse {
    private Long batteryId;
    private String serialNumber;
    private Double oldSoH;
    private Double newSoH;
    private String planName;
    private Double planMinSoH;
    private Double planMaxSoH;
    private String message;
}
