package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.response.VNPayCallbackResult;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VNPayService {

    /**
     * Tạo URL thanh toán VNPay cho invoice
     * @param invoiceId ID của invoice cần thanh toán
     * @param ipAddress IP của client
     * @return URL redirect đến VNPay
     */
    String createPaymentUrl(Long invoiceId, String ipAddress);

    /**
     * Xác thực và xử lý callback từ VNPay
     * @param params Query parameters từ VNPay return
     * @return VNPayCallbackResult chứa thông tin invoice và kết quả thanh toán
     */
    VNPayCallbackResult processVNPayCallback(Map<String, String> params);

    /**
     * Lấy client IP address từ request
     * @param request HTTP request
     * @return IP address
     */
    String getIpAddress(HttpServletRequest request);
}
