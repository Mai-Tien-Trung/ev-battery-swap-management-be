package com.evstation.batteryswap.dto.response;


import lombok.Data;

@Data
public class VehicleResponse {
    private Long id;
    private String vin;
    private String model;
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