package com.evstation.batteryswap.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for VnpayConfig utility class
 */
@ExtendWith(MockitoExtension.class)
class VnpayConfigTest {

    @InjectMocks
    private VnpayConfig vnpayConfig;

    @BeforeEach
    void setUp() {
        // Set test values using reflection
        ReflectionTestUtils.setField(vnpayConfig, "vnpPayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnpayConfig, "vnpReturnUrl", "http://localhost:8080/api/payment/vnpay-return");
        ReflectionTestUtils.setField(vnpayConfig, "vnpTmnCode", "test_tmn_code");
        ReflectionTestUtils.setField(vnpayConfig, "secretKey", "test_secret_key");
        ReflectionTestUtils.setField(vnpayConfig, "vnpApiUrl", "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");
    }

    @Test
    void testMd5() {
        // Arrange
        String input = "test message";
        
        // Act
        String result = vnpayConfig.md5(input);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(32, result.length()); // MD5 hash is 32 characters
    }

    @Test
    void testMd5_EmptyString() {
        // Arrange
        String input = "";
        
        // Act
        String result = vnpayConfig.md5(input);
        
        // Assert
        assertNotNull(result);
        assertEquals(32, result.length());
    }

    @Test
    void testSha256() {
        // Arrange
        String input = "test message";
        
        // Act
        String result = vnpayConfig.sha256(input);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(64, result.length()); // SHA256 hash is 64 characters
    }

    @Test
    void testSha256_EmptyString() {
        // Arrange
        String input = "";
        
        // Act
        String result = vnpayConfig.sha256(input);
        
        // Assert
        assertNotNull(result);
        assertEquals(64, result.length());
    }

    @Test
    void testHmacSHA512() {
        // Arrange
        String key = "test_secret_key";
        String data = "test data";
        
        // Act
        String result = vnpayConfig.hmacSHA512(key, data);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(128, result.length()); // HMAC SHA512 is 128 characters
    }

    @Test
    void testHmacSHA512_NullKey() {
        // Arrange
        String key = null;
        String data = "test data";
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> vnpayConfig.hmacSHA512(key, data));
    }

    @Test
    void testHmacSHA512_NullData() {
        // Arrange
        String key = "test_secret_key";
        String data = null;
        
        // Act & Assert
        assertThrows(NullPointerException.class, () -> vnpayConfig.hmacSHA512(key, data));
    }

    @Test
    void testGetIpAddress_WithXForwardedFor() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn("192.168.1.1");
        
        // Act
        String result = vnpayConfig.getIpAddress((jakarta.servlet.http.HttpServletRequest) request);
        
        // Assert
        assertEquals("192.168.1.1", result);
    }

    @Test
    void testGetIpAddress_WithRemoteAddr() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // Act
        String result = vnpayConfig.getIpAddress((jakarta.servlet.http.HttpServletRequest) request);
        
        // Assert
        assertEquals("127.0.0.1", result);
    }

    @Test
    void testGetIpAddress_Exception() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-FORWARDED-FOR")).thenThrow(new RuntimeException("Test exception"));
        
        // Act
        String result = vnpayConfig.getIpAddress((jakarta.servlet.http.HttpServletRequest) request);
        
        // Assert
        assertTrue(result.startsWith("Invalid IP:"));
    }

    @Test
    void testGetRandomNumber() {
        // Arrange
        int length = 8;
        
        // Act
        String result = vnpayConfig.getRandomNumber(length);
        
        // Assert
        assertNotNull(result);
        assertEquals(length, result.length());
        assertTrue(result.matches("\\d+"));
    }

    @Test
    void testGetRandomNumber_DifferentLengths() {
        // Test different lengths
        for (int length = 1; length <= 10; length++) {
            String result = vnpayConfig.getRandomNumber(length);
            assertEquals(length, result.length());
            assertTrue(result.matches("\\d+"));
        }
    }

    @Test
    void testBuildPaymentUrl() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", "test_tmn");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", "12345678");
        params.put("vnp_OrderInfo", "Test payment");
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", "http://localhost:8080/api/payment/vnpay-return");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", "20231201120000");
        params.put("vnp_ExpireDate", "20231201121500");
        
        // Act
        String result = vnpayConfig.buildPaymentUrl(params);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(result.contains("vnp_SecureHash="));
    }

    @Test
    void testVerifySignature_ValidSignature() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "12345678");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "1234567890");
        params.put("vnp_SecureHash", "test_hash");
        
        // Mock the hmacSHA512 method to return a predictable result
        VnpayConfig spyConfig = spy(vnpayConfig);
        doReturn("test_hash").when(spyConfig).hmacSHA512(anyString(), anyString());
        
        // Act
        boolean result = spyConfig.verifySignature(params);
        
        // Assert
        assertTrue(result);
    }

    @Test
    void testVerifySignature_InvalidSignature() {
        // Arrange
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "12345678");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "1234567890");
        params.put("vnp_SecureHash", "invalid_hash");
        
        // Mock the hmacSHA512 method to return a different result
        VnpayConfig spyConfig = spy(vnpayConfig);
        doReturn("different_hash").when(spyConfig).hmacSHA512(anyString(), anyString());
        
        // Act
        boolean result = spyConfig.verifySignature(params);
        
        // Assert
        assertFalse(result);
    }

    @Test
    void testGetters() {
        // Test all getter methods
        assertEquals("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", vnpayConfig.getVnpPayUrl());
        assertEquals("http://localhost:8080/api/payment/vnpay-return", vnpayConfig.getVnpReturnUrl());
        assertEquals("test_tmn_code", vnpayConfig.getVnpTmnCode());
        assertEquals("test_secret_key", vnpayConfig.getSecretKey());
        assertEquals("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction", vnpayConfig.getVnpApiUrl());
        assertEquals("2.1.0", vnpayConfig.getVnpVersion());
        assertEquals("pay", vnpayConfig.getVnpCommand());
        assertEquals("other", vnpayConfig.getOrderType());
        assertEquals("VND", vnpayConfig.getCurrencyCode());
        assertEquals("vn", vnpayConfig.getDefaultLocale());
    }
}
