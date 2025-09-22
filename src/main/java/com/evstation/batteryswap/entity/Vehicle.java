package com.evstation.batteryswap.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
@Data
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vin;

    @Column(nullable = false)
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
    @ManyToMany(mappedBy = "vehicles")
    private List<User> users = new ArrayList<>();

    // Xe có thể có nhiều subscription
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    private List<Subscription> subscriptions = new ArrayList<>();
}

