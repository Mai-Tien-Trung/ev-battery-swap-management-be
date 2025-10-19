package com.evstation.batteryswap.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * VNPAY Configuration and utility class
 * Handles VNPAY payment integration utilities
 */
@Component
public class VnpayConfig {

    @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    @Value("${vnpay.returnUrl:http://localhost:8080/api/payment/vnpay-return}")
    private String vnpReturnUrl;

    // TODO: Replace with your actual VNPAY provided tmnCode here
    @Value("${vnpay.tmnCode:}")
    private String vnpTmnCode;

    // TODO: Replace with your actual VNPAY provided secretKey here
    // Critical: This must match exactly the secret key from VNPAY
    @Value("${vnpay.secretKey:}")
    private String secretKey;

    @Value("${vnpay.apiUrl:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String vnpApiUrl;

    private static final String VNP_VERSION = "2.1.0";
    private static final String VNP_COMMAND = "pay";
    private static final String ORDER_TYPE = "other";
    private static final String CURRENCY_CODE = "VND";
    private static final String DEFAULT_LOCALE = "vn";

    /**
     * Generate MD5 hash for the given message
     */
    public String md5(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            digest = sb.toString();
        } catch (Exception ex) {
            digest = "";
        }
        return digest;
    }

    /**
     * Generate SHA256 hash for the given message
     */
    public String sha256(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            digest = sb.toString();
        } catch (Exception ex) {
            digest = "";
        }
        return digest;
    }

    /**
     * Generate HMAC SHA512 hash for VNPAY signature
     */
    public String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Get client IP address from request
     */
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAddress = "Invalid IP:" + e.getMessage();
        }
        return ipAddress;
    }

    /**
     * Generate random number string with specified length
     */
    public String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Build VNPAY payment URL with all required parameters
     */
    public String buildPaymentUrl(Map<String, String> vnpParams) {
        // Sort parameters alphabetically
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParams.get(fieldName);
            
            if (fieldValue != null && fieldValue.length() > 0) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(java.net.URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                // Build query
                query.append(java.net.URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(java.net.URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                
                if (i < fieldNames.size() - 1) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnpSecureHash = hmacSHA512(secretKey, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        
        return vnpPayUrl + "?" + queryUrl;
    }

    /**
     * Verify VNPAY callback signature
     */
    public boolean verifySignature(Map<String, String> params) {
        String vnpSecureHash = params.remove("vnp_SecureHash");
        
        // Sort parameters alphabetically
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        
        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = params.get(fieldName);
            
            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);
                
                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                }
            }
        }
        
        String calculatedHash = hmacSHA512(secretKey, hashData.toString());
        return calculatedHash.equals(vnpSecureHash);
    }

    // Getters for configuration values
    public String getVnpPayUrl() {
        return vnpPayUrl;
    }

    public String getVnpReturnUrl() {
        return vnpReturnUrl;
    }

    public String getVnpTmnCode() {
        return vnpTmnCode;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getVnpApiUrl() {
        return vnpApiUrl;
    }

    public String getVnpVersion() {
        return VNP_VERSION;
    }

    public String getVnpCommand() {
        return VNP_COMMAND;
    }

    public String getOrderType() {
        return ORDER_TYPE;
    }

    public String getCurrencyCode() {
        return CURRENCY_CODE;
    }

    public String getDefaultLocale() {
        return DEFAULT_LOCALE;
    }
}
