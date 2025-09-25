package com.evstation.batteryswap.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "swap_transaction")
public class SwapTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "old_battery_id")
    private Battery oldBattery;

    @ManyToOne
    @JoinColumn(name = "new_battery_id")
    private Battery newBattery;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Station station;

    private LocalDateTime timestamp;
}

