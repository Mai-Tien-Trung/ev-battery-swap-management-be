package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModelResponse {
    private Long id;
    private String name;
    private String brand;
    private String brakeSystem;
    private Double weightWithBattery;
}
