package com.evstation.batteryswap.service;

import com.evstation.batteryswap.entity.Payment;
import com.evstation.batteryswap.entity.Payment.PaymentStatus;

/**
 * Service interface for battery swap operations including payment processing
 */
public interface BatterySwapService {

    /**
     * Initiate payment for battery swap service
     * @param amount payment amount in VND
     * @param orderInfo order description
     * @param bankCode optional bank code
     * @param language language preference
     * @param ipAddress client IP address
     * @return transaction reference
     */
    String initiatePayment(Long amount, String orderInfo, String bankCode, String language, String ipAddress);

    /**
     * Update payment status based on VNPAY callback
     * @param txnRef transaction reference
     * @param status new payment status
     * @param vnpayTransactionId VNPAY transaction ID
     * @param vnpayResponseCode VNPAY response code
     * @return updated payment entity
     */
    Payment updatePaymentStatus(String txnRef, PaymentStatus status, String vnpayTransactionId, String vnpayResponseCode);

    /**
     * Get payment by transaction reference
     * @param txnRef transaction reference
     * @return payment entity
     */
    Payment getPaymentByTxnRef(String txnRef);

    /**
     * Calculate payment amount based on service parameters
     * This method can be extended to calculate amount based on:
     * - Charging time
     * - Battery capacity
     * - Service type
     * - Subscription plan
     * @param chargingMinutes charging duration in minutes
     * @param rate rate per minute
     * @return calculated amount
     */
    Long calculatePaymentAmount(Integer chargingMinutes, Long rate);
}
