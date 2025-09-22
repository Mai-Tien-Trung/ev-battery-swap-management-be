package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleSummaryResponse {
    private Long id;
    private String vin;
    private String model;
}
