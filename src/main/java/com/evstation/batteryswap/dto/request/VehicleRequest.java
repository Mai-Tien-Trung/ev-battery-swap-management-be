package com.evstation.batteryswap.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VehicleRequest {
    @NotBlank(message = "VIN cannot be blank")
    private String vin;

    @NotBlank(message = "Model cannot be blank")
    private String model;

    @NotBlank(message = "Chiều dài cơ sở không được để trống")
    private String wheelbase;

    @NotBlank(message = "Khoảng sáng gầm không được để trống")
    private String groundClearance;

    @NotBlank(message = "Chiều cao yên trước không được để trống")
    private String seatHeight;

    @NotBlank(message = "Kích thước bánh trước không được để trống")
    private String frontTire;

    @NotBlank(message = "Kích thước bánh sau không được để trống")
    private String rearTire;

    @NotBlank(message = "Giảm sóc trước không được để trống")
    private String frontSuspension;

    @NotBlank(message = "Giảm sóc sau không được để trống")
    private String rearSuspension;

    @NotBlank(message = "Phanh không được để trống")
    private String brakeSystem;

    @NotBlank(message = "Dung tích cốp không được để trống")
    private String trunkCapacity;

    @NotNull(message = "Khối lượng không pin không được null")
    @Positive(message = "Khối lượng phải > 0")
    private Double weightWithoutBattery;

    @NotNull(message = "Khối lượng với pin không được null")
    @Positive(message = "Khối lượng phải > 0")
    private Double weightWithBattery;
}
