package com.evstation.batteryswap.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "swap_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người dùng thực hiện swap
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Xe đang gắn pin
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
    private Double energyUsed;
    private Double distance;
    private Double cost;
    // Pin thật được sử dụng trong giao dịch
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battery_serial_id", nullable = false)
    private BatterySerial batterySerial;

    // Trạm diễn ra swap (có thể null nếu là phát pin ban đầu)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    // Thời điểm thực hiện
    private LocalDateTime timestamp = LocalDateTime.now();

    // ⚡️ Các trường phục vụ tính hao mòn pin
    private Double startPercent;       // phần trăm lúc nhận pin (VD: 100%)
    private Double endPercent;         // phần trăm lúc trả pin (VD: 20%)
    private Double depthOfDischarge;   // DoD = start - end
    private Double degradationThisSwap; // mức hao mòn % trong giao dịch này
}
