package com.evstation.batteryswap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "batteries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Battery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Ví dụ: Lithium 72V - 40Ah

    private String type; // xe máy / xe tải / xe đạp điện
    private Double designCapacity; // Wh hoặc mAh
    private String description;

    @OneToMany(mappedBy = "battery", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BatterySerial> serials = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
}
