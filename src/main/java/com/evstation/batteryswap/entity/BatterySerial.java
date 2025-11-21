package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.BatteryStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "battery_serials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatterySerial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String serialNumber; // Ví dụ: BAT-001

    @Enumerated(EnumType.STRING)
    private BatteryStatus status;



    private Double initialCapacity;
    private Double currentCapacity;
    private Double stateOfHealth;     // SoH = (current / initial) * 100 (%)
    private Double totalCycleCount = 0.0; // Tổng số chu kỳ sử dụng tương đương (EFC)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battery_id")
    private Battery battery; // liên kết loại pin (model)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station; // trạm hiện tại

    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle; // xe hiện đang giữ pin
    @Column(name = "charge_percent")
    private Double chargePercent = 100.0;

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
