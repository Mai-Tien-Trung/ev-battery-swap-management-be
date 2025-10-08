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
    @Column(nullable = false)
    private BatteryStatus status = BatteryStatus.AVAILABLE;

    @Column(nullable = false)
    private int swapCount = 0;

    // 🔋 Thêm các trường liên quan đến hao mòn
    private Double initialCapacity;   // Dung lượng thiết kế ban đầu (mAh hoặc Wh)
    private Double currentCapacity;   // Dung lượng hiện tại đo được
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

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
