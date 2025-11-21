package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Invoice;
import com.evstation.batteryswap.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Tìm invoice theo subscription và status
    List<Invoice> findBySubscriptionIdAndStatus(Long subscriptionId, InvoiceStatus status);

    // Tìm tất cả invoice PENDING của một subscription
    @Query("SELECT i FROM Invoice i WHERE i.subscription.id = :subscriptionId AND i.status = 'PENDING'")
    List<Invoice> findPendingBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    // Tìm invoice theo swap transaction
    Optional<Invoice> findBySwapTransactionId(Long swapTransactionId);

    // Tìm tất cả invoice của user
    @Query("SELECT i FROM Invoice i WHERE i.subscription.user.id = :userId ORDER BY i.createdAt DESC")
    List<Invoice> findByUserId(@Param("userId") Long userId);

    // Kiểm tra có invoice PENDING của subscription không
    @Query("SELECT COUNT(i) > 0 FROM Invoice i WHERE i.subscription.id = :subscriptionId AND i.status = 'PENDING'")
    boolean hasPendingInvoices(@Param("subscriptionId") Long subscriptionId);
}
