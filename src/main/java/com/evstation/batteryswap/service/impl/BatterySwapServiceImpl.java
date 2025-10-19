package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.entity.Payment;
import com.evstation.batteryswap.entity.Payment.PaymentStatus;
import com.evstation.batteryswap.repository.PaymentRepository;
import com.evstation.batteryswap.service.BatterySwapService;
import com.evstation.batteryswap.utils.VnpayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementation of BatterySwapService
 * Handles battery swap operations and payment processing
 */
@Service
public class BatterySwapServiceImpl implements BatterySwapService {

    private final PaymentRepository paymentRepository;
    private final VnpayConfig vnpayConfig;

    @Autowired
    public BatterySwapServiceImpl(PaymentRepository paymentRepository, VnpayConfig vnpayConfig) {
        this.paymentRepository = paymentRepository;
        this.vnpayConfig = vnpayConfig;
    }

    @Override
    public String initiatePayment(Long amount, String orderInfo, String bankCode, String language, String ipAddress) {
        // Generate unique transaction reference
        String txnRef = vnpayConfig.getRandomNumber(8);
        
        // Ensure txnRef is unique
        while (paymentRepository.existsByTxnRef(txnRef)) {
            txnRef = vnpayConfig.getRandomNumber(8);
        }

        // Create payment record
        Payment payment = Payment.builder()
                .txnRef(txnRef)
                .amount(amount)
                .orderInfo(orderInfo != null ? orderInfo : "Thanh toan dich vu sac pin:" + txnRef)
                .bankCode(bankCode)
                .language(language != null ? language : vnpayConfig.getDefaultLocale())
                .ipAddress(ipAddress)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        
        return txnRef;
    }

    @Override
    public Payment updatePaymentStatus(String txnRef, PaymentStatus status, String vnpayTransactionId, String vnpayResponseCode) {
        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Payment not found with txnRef: " + txnRef));

        payment.setStatus(status);
        payment.setVnpayTransactionId(vnpayTransactionId);
        payment.setVnpayResponseCode(vnpayResponseCode);
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Override
    public Payment getPaymentByTxnRef(String txnRef) {
        return paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Payment not found with txnRef: " + txnRef));
    }

    @Override
    public Long calculatePaymentAmount(Integer chargingMinutes, Long rate) {
        if (chargingMinutes == null || chargingMinutes <= 0) {
            throw new IllegalArgumentException("Charging minutes must be positive");
        }
        if (rate == null || rate <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        
        // Calculate amount based on charging time and rate
        // This can be extended with more complex pricing logic
        return (long) chargingMinutes * rate;
    }
}
