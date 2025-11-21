package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.PaymentTransaction;
import com.evstation.batteryswap.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    // Tìm transaction theo mã giao dịch VNPay
    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);

    // Tìm tất cả transaction của invoice
    List<PaymentTransaction> findByInvoiceId(Long invoiceId);

    // Tìm transaction theo status
    List<PaymentTransaction> findByStatus(PaymentStatus status);

    // Tìm transaction của invoice theo status
    Optional<PaymentTransaction> findByInvoiceIdAndStatus(Long invoiceId, PaymentStatus status);
}
