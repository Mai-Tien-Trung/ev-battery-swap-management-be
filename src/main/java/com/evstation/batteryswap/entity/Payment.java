package com.evstation.batteryswap.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Payment entity for VNPAY transactions
 * Stores payment information and status
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "txn_ref", unique = true, nullable = false)
    private String txnRef;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "order_info")
    private String orderInfo;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "language")
    private String language;

    @Column(name = "ip_address")
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "vnpay_transaction_id")
    private String vnpayTransactionId;

    @Column(name = "vnpay_response_code")
    private String vnpayResponseCode;

    @Column(name = "vnpay_secure_hash")
    private String vnpaySecureHash;

    @Column(name = "created_at", nullable = false)
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
     * Payment status enumeration
     */
    public enum PaymentStatus {
        PENDING,
        SUCCESS,
        FAILED,
        CANCELLED
    }
}
