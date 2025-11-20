package com.evstation.batteryswap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Chi tiết pin trong mỗi reservation
 * 
 * Relationship: Reservation (1) ←→ (N) ReservationItem ←→ (1) BatterySerial
 * 
 * Ví dụ:
 * Reservation #10 (user đặt 2 pin):
 *   - ReservationItem #1 → Battery #101 (BAT-001)
 *   - ReservationItem #2 → Battery #102 (BAT-002)
 */
@Entity
@Table(name = "reservation_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reservation chứa item này
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    /**
     * Pin được đặt trước
     * Pin này sẽ có status = RESERVED trong thời gian reservation ACTIVE
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battery_serial_id", nullable = false)
    private BatterySerial batterySerial;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
