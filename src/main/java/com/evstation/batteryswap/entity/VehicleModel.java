package com.evstation.batteryswap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicle_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
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

    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL)
    private List<Vehicle> vehicles = new ArrayList<>();
}
