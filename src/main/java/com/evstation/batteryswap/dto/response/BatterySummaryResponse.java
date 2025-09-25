package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatterySummaryResponse {
    private Long id;
    private String serialNumber;
    private String status;
}
