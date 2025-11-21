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
    private String wheelbase;
    private String groundClearance;
    private String seatHeight;
    private String frontTire;
    private String rearTire;
    private String frontSuspension;
    private String rearSuspension;
    private String brakeSystem;
    private String trunkCapacity;
    private Double weightWithoutBattery;
    private Double weightWithBattery;
}
