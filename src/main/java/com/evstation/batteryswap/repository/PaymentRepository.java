package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Payment entity
 * Handles database operations for payment transactions
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by transaction reference
     * @param txnRef transaction reference
     * @return Optional containing payment if found
     */
    Optional<Payment> findByTxnRef(String txnRef);

    /**
     * Check if payment exists by transaction reference
     * @param txnRef transaction reference
     * @return true if payment exists
     */
    boolean existsByTxnRef(String txnRef);
}
