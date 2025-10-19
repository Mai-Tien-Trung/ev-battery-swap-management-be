package com.evstation.batteryswap.service;

import com.evstation.batteryswap.entity.Payment;
import com.evstation.batteryswap.entity.Payment.PaymentStatus;
import com.evstation.batteryswap.repository.PaymentRepository;
import com.evstation.batteryswap.service.impl.BatterySwapServiceImpl;
import com.evstation.batteryswap.utils.VnpayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for BatterySwapService
 */
@ExtendWith(MockitoExtension.class)
class BatterySwapServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private VnpayConfig vnpayConfig;

    @InjectMocks
    private BatterySwapServiceImpl batterySwapService;

    @BeforeEach
    void setUp() {
        // Setup common mocks
        when(vnpayConfig.getDefaultLocale()).thenReturn("vn");
    }

    @Test
    void testInitiatePayment_Success() {
        // Arrange
        Long amount = 100000L;
        String orderInfo = "Test payment";
        String bankCode = "NCB";
        String language = "vn";
        String ipAddress = "127.0.0.1";
        String txnRef = "12345678";

        when(vnpayConfig.getRandomNumber(8)).thenReturn(txnRef);
        when(paymentRepository.existsByTxnRef(txnRef)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });

        // Act
        String result = batterySwapService.initiatePayment(amount, orderInfo, bankCode, language, ipAddress);

        // Assert
        assertEquals(txnRef, result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testInitiatePayment_WithDuplicateTxnRef() {
        // Arrange
        Long amount = 100000L;
        String orderInfo = "Test payment";
        String bankCode = "NCB";
        String language = "vn";
        String ipAddress = "127.0.0.1";
        String txnRef1 = "12345678";
        String txnRef2 = "87654321";

        when(vnpayConfig.getRandomNumber(8))
                .thenReturn(txnRef1)
                .thenReturn(txnRef2);
        when(paymentRepository.existsByTxnRef(txnRef1)).thenReturn(true);
        when(paymentRepository.existsByTxnRef(txnRef2)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });

        // Act
        String result = batterySwapService.initiatePayment(amount, orderInfo, bankCode, language, ipAddress);

        // Assert
        assertEquals(txnRef2, result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testInitiatePayment_WithDefaultValues() {
        // Arrange
        Long amount = 100000L;
        String txnRef = "12345678";

        when(vnpayConfig.getRandomNumber(8)).thenReturn(txnRef);
        when(paymentRepository.existsByTxnRef(txnRef)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });

        // Act
        String result = batterySwapService.initiatePayment(amount, null, null, null, "127.0.0.1");

        // Assert
        assertEquals(txnRef, result);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testUpdatePaymentStatus_Success() {
        // Arrange
        String txnRef = "12345678";
        PaymentStatus status = PaymentStatus.SUCCESS;
        String vnpayTransactionId = "1234567890";
        String vnpayResponseCode = "00";

        Payment existingPayment = Payment.builder()
                .id(1L)
                .txnRef(txnRef)
                .amount(100000L)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByTxnRef(txnRef)).thenReturn(java.util.Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Payment result = batterySwapService.updatePaymentStatus(txnRef, status, vnpayTransactionId, vnpayResponseCode);

        // Assert
        assertNotNull(result);
        assertEquals(status, result.getStatus());
        assertEquals(vnpayTransactionId, result.getVnpayTransactionId());
        assertEquals(vnpayResponseCode, result.getVnpayResponseCode());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void testUpdatePaymentStatus_PaymentNotFound() {
        // Arrange
        String txnRef = "12345678";
        PaymentStatus status = PaymentStatus.SUCCESS;
        String vnpayTransactionId = "1234567890";
        String vnpayResponseCode = "00";

        when(paymentRepository.findByTxnRef(txnRef)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            batterySwapService.updatePaymentStatus(txnRef, status, vnpayTransactionId, vnpayResponseCode);
        });
    }

    @Test
    void testGetPaymentByTxnRef_Success() {
        // Arrange
        String txnRef = "12345678";
        Payment payment = Payment.builder()
                .id(1L)
                .txnRef(txnRef)
                .amount(100000L)
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByTxnRef(txnRef)).thenReturn(java.util.Optional.of(payment));

        // Act
        Payment result = batterySwapService.getPaymentByTxnRef(txnRef);

        // Assert
        assertNotNull(result);
        assertEquals(txnRef, result.getTxnRef());
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
    }

    @Test
    void testGetPaymentByTxnRef_NotFound() {
        // Arrange
        String txnRef = "12345678";
        when(paymentRepository.findByTxnRef(txnRef)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            batterySwapService.getPaymentByTxnRef(txnRef);
        });
    }

    @Test
    void testCalculatePaymentAmount_Success() {
        // Arrange
        Integer chargingMinutes = 30;
        Long rate = 1000L;
        Long expectedAmount = 30000L;

        // Act
        Long result = batterySwapService.calculatePaymentAmount(chargingMinutes, rate);

        // Assert
        assertEquals(expectedAmount, result);
    }

    @Test
    void testCalculatePaymentAmount_InvalidChargingMinutes() {
        // Arrange
        Integer chargingMinutes = 0;
        Long rate = 1000L;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            batterySwapService.calculatePaymentAmount(chargingMinutes, rate);
        });
    }

    @Test
    void testCalculatePaymentAmount_InvalidRate() {
        // Arrange
        Integer chargingMinutes = 30;
        Long rate = 0L;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            batterySwapService.calculatePaymentAmount(chargingMinutes, rate);
        });
    }

    @Test
    void testCalculatePaymentAmount_NullChargingMinutes() {
        // Arrange
        Integer chargingMinutes = null;
        Long rate = 1000L;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            batterySwapService.calculatePaymentAmount(chargingMinutes, rate);
        });
    }

    @Test
    void testCalculatePaymentAmount_NullRate() {
        // Arrange
        Integer chargingMinutes = 30;
        Long rate = null;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            batterySwapService.calculatePaymentAmount(chargingMinutes, rate);
        });
    }
}
