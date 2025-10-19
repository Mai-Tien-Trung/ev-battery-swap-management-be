package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.entity.Payment;
import com.evstation.batteryswap.entity.Payment.PaymentStatus;
import com.evstation.batteryswap.service.BatterySwapService;
import com.evstation.batteryswap.utils.VnpayConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test cases for PaymentController
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VnpayConfig vnpayConfig;

    @MockBean
    private BatterySwapService batterySwapService;

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new Gson();
    }

    @Test
    void testInitPayment_Success() throws Exception {
        // Arrange
        String txnRef = "12345678";
        String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?test=params";
        
        when(vnpayConfig.getIpAddress(any())).thenReturn("127.0.0.1");
        when(vnpayConfig.getVnpVersion()).thenReturn("2.1.0");
        when(vnpayConfig.getVnpCommand()).thenReturn("pay");
        when(vnpayConfig.getVnpTmnCode()).thenReturn("test_tmn");
        when(vnpayConfig.getCurrencyCode()).thenReturn("VND");
        when(vnpayConfig.getOrderType()).thenReturn("other");
        when(vnpayConfig.getDefaultLocale()).thenReturn("vn");
        when(vnpayConfig.getVnpReturnUrl()).thenReturn("http://localhost:8080/api/payment/vnpay-return");
        when(vnpayConfig.buildPaymentUrl(any())).thenReturn(paymentUrl);
        when(batterySwapService.initiatePayment(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(txnRef);

        // Act & Assert
        mockMvc.perform(post("/api/payment/init")
                .param("amount", "100000")
                .param("bankCode", "NCB")
                .param("language", "vn")
                .param("orderInfo", "Test payment"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").value(paymentUrl));

        verify(batterySwapService).initiatePayment(eq(100000L), eq("Test payment"), eq("NCB"), eq("vn"), eq("127.0.0.1"));
    }

    @Test
    void testInitPayment_InvalidAmount() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/payment/init")
                .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("99"))
                .andExpect(jsonPath("$.message").value("Invalid amount"));

        verify(batterySwapService, never()).initiatePayment(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testInitPayment_MissingAmount() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/payment/init"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("99"))
                .andExpect(jsonPath("$.message").value("Invalid amount"));

        verify(batterySwapService, never()).initiatePayment(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testInitPayment_Exception() throws Exception {
        // Arrange
        when(vnpayConfig.getIpAddress(any())).thenReturn("127.0.0.1");
        when(batterySwapService.initiatePayment(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(post("/api/payment/init")
                .param("amount", "100000"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("99"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Payment initialization failed")));
    }

    @Test
    void testVnpayReturn_Success() throws Exception {
        // Arrange
        String txnRef = "12345678";
        Payment payment = Payment.builder()
                .id(1L)
                .txnRef(txnRef)
                .amount(100000L)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "1234567890");
        params.put("vnp_SecureHash", "test_hash");

        when(vnpayConfig.verifySignature(any())).thenReturn(true);
        when(batterySwapService.getPaymentByTxnRef(txnRef)).thenReturn(payment);
        when(batterySwapService.updatePaymentStatus(anyString(), any(PaymentStatus.class), anyString(), anyString()))
                .thenReturn(payment);

        // Act & Assert
        mockMvc.perform(get("/api/payment/vnpay-return")
                .param("vnp_TxnRef", txnRef)
                .param("vnp_ResponseCode", "00")
                .param("vnp_TransactionNo", "1234567890")
                .param("vnp_SecureHash", "test_hash"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.message").value("Payment successful"))
                .andExpect(jsonPath("$.txnRef").value(txnRef))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(batterySwapService).updatePaymentStatus(eq(txnRef), eq(PaymentStatus.SUCCESS), eq("1234567890"), eq("00"));
    }

    @Test
    void testVnpayReturn_InvalidSignature() throws Exception {
        // Arrange
        when(vnpayConfig.verifySignature(any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/payment/vnpay-return")
                .param("vnp_TxnRef", "12345678")
                .param("vnp_ResponseCode", "00")
                .param("vnp_SecureHash", "invalid_hash"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("99"))
                .andExpect(jsonPath("$.message").value("Invalid signature"));

        verify(batterySwapService, never()).updatePaymentStatus(anyString(), any(PaymentStatus.class), anyString(), anyString());
    }

    @Test
    void testVnpayReturn_PaymentFailed() throws Exception {
        // Arrange
        String txnRef = "12345678";
        Payment payment = Payment.builder()
                .id(1L)
                .txnRef(txnRef)
                .amount(100000L)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(vnpayConfig.verifySignature(any())).thenReturn(true);
        when(batterySwapService.getPaymentByTxnRef(txnRef)).thenReturn(payment);
        when(batterySwapService.updatePaymentStatus(anyString(), any(PaymentStatus.class), anyString(), anyString()))
                .thenReturn(payment);

        // Act & Assert
        mockMvc.perform(get("/api/payment/vnpay-return")
                .param("vnp_TxnRef", txnRef)
                .param("vnp_ResponseCode", "07")
                .param("vnp_TransactionNo", "1234567890")
                .param("vnp_SecureHash", "test_hash"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("07"))
                .andExpect(jsonPath("$.message").value("Payment failed"))
                .andExpect(jsonPath("$.status").value("FAILED"));

        verify(batterySwapService).updatePaymentStatus(eq(txnRef), eq(PaymentStatus.FAILED), eq("1234567890"), eq("07"));
    }

    @Test
    void testGetPaymentStatus_Success() throws Exception {
        // Arrange
        String txnRef = "12345678";
        Payment payment = Payment.builder()
                .id(1L)
                .txnRef(txnRef)
                .amount(100000L)
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(batterySwapService.getPaymentByTxnRef(txnRef)).thenReturn(payment);

        // Act & Assert
        mockMvc.perform(get("/api/payment/status/{txnRef}", txnRef))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.txnRef").value(txnRef))
                .andExpect(jsonPath("$.amount").value(100000))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testGetPaymentStatus_NotFound() throws Exception {
        // Arrange
        String txnRef = "12345678";
        when(batterySwapService.getPaymentByTxnRef(txnRef))
                .thenThrow(new RuntimeException("Payment not found with txnRef: " + txnRef));

        // Act & Assert
        mockMvc.perform(get("/api/payment/status/{txnRef}", txnRef))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("99"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Payment not found")));
    }
}
