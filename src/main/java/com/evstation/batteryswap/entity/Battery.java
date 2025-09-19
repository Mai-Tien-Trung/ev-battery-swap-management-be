package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.BatteryStatus;
import jakarta.persistence.*;
import lombok.*;

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
    private String serialNumber; // mã pin duy nhất

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatteryStatus status = BatteryStatus.AVAILABLE;

    @Column(nullable = false)
    private int swapCount = 0; // số lần đã đổi

    // FK: Pin hiện đang ở trạm nào
    @ManyToOne
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;
}
