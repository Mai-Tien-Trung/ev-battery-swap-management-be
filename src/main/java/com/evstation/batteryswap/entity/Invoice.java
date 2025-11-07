package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.InvoiceStatus;
import com.evstation.batteryswap.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Subscription liên quan
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    // Swap transaction gây ra hóa đơn này (nullable - chỉ có cho swap overage invoice)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_transaction_id")
    private SwapTransaction swapTransaction;

    // Số tiền phải trả
    @Column(nullable = false)
    private Double amount;

    // Trạng thái thanh toán
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    // Loại invoice: "SWAP_OVERAGE" hoặc "SUBSCRIPTION_RENEWAL"
    @Column(name = "invoice_type", length = 50)
    private String invoiceType;

    // Loại usage (DISTANCE hoặc ENERGY) - chỉ dùng cho SWAP_OVERAGE
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type")
    private PlanType usageType;

    // Số lượng vượt (km hoặc kWh) - chỉ dùng cho SWAP_OVERAGE
    @Column
    private Double overage;

    // Đơn giá tier rate (VND/km hoặc VND/kWh) - chỉ dùng cho SWAP_OVERAGE
    @Column
    private Double rate;

    // Mô tả chi tiết: "Overage: 1.5 kWh × 13,826₫/kWh = 20,739₫"
    @Column(length = 500)
    private String description;

    // Thời gian tạo hóa đơn
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Thời gian thanh toán (null nếu chưa trả)
    private LocalDateTime paidAt;
}
