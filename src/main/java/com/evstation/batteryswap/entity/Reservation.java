package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity đại diện cho việc đặt trước pin của user
 * 
 * Business Flow:
 * 1. User tạo reservation → Pin được lock (RESERVED)
 * 2. Trong 1 giờ, user có thể đến swap
 * 3. Nếu swap thành công → status = USED
 * 4. Nếu quá 1 giờ chưa swap → status = EXPIRED (auto-expire job)
 * 5. Nếu user hủy → status = CANCELLED
 * 
 * Mỗi vehicle chỉ có 1 reservation ACTIVE tại 1 thời điểm
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User đặt pin
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Phương tiện cần đổi pin
     * ⚠️ BẮT BUỘC - Mỗi vehicle chỉ có 1 reservation ACTIVE
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /**
     * Trạm đặt pin
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    /**
     * Subscription đang dùng (để validate plan limit)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /**
     * Trạng thái reservation
     * ACTIVE → USED/EXPIRED/CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    /**
     * Số lượng pin đặt
     * Phải <= maxBatteries của plan
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Thời điểm đặt pin
     */
    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    /**
     * Thời điểm hết hạn (reserved_at + 1 hour)
     * ⏱️ Sau thời điểm này, auto-expire job sẽ chuyển status → EXPIRED
     */
    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    /**
     * Thời điểm thực tế sử dụng (khi swap thành công)
     * Chỉ có giá trị khi status = USED
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Swap transaction đã sử dụng reservation này
     * Link với swap_transactions để tracking
     */
    @Column(name = "swap_transaction_id")
    private Long swapTransactionId;

    /**
     * Thời điểm hủy/hết hạn
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Lý do hủy (nếu status = CANCELLED)
     */
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    /**
     * Danh sách pin được đặt
     * OneToMany với ReservationItem
     */
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Helper method: Kiểm tra reservation còn hiệu lực không
     */
    public boolean isActive() {
        return status == ReservationStatus.ACTIVE 
            && LocalDateTime.now().isBefore(expireAt);
    }

    /**
     * Helper method: Tính thời gian còn lại (phút)
     */
    public long getRemainingMinutes() {
        if (status != ReservationStatus.ACTIVE) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expireAt)) {
            return 0;
        }
        return java.time.Duration.between(now, expireAt).toMinutes();
    }
}
