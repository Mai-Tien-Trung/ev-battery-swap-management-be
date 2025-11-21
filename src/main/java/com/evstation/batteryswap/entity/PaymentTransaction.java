package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Invoice liên kết
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Mã giao dịch VNPay (vnp_TxnRef)
    @Column(nullable = false, unique = true, length = 100)
    private String transactionRef;

    // Số tiền thanh toán (VND)
    @Column(nullable = false)
    private Double amount;

    // Trạng thái thanh toán
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    // Mã giao dịch VNPay trả về (vnp_TransactionNo)
    @Column(length = 100)
    private String vnpTransactionNo;

    // Mã ngân hàng (vnp_BankCode)
    @Column(length = 20)
    private String bankCode;

    // Response code từ VNPay (00 = success)
    @Column(length = 10)
    private String responseCode;

    // Thông tin bổ sung từ VNPay
    @Column(length = 500)
    private String orderInfo;

    // Thời gian tạo
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Thời gian VNPay phản hồi
    private LocalDateTime paidAt;

    // Secure hash từ VNPay (để verify)
    @Column(length = 500)
    private String secureHash;
}
