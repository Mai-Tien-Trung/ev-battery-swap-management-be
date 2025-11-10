package com.evstation.batteryswap.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VNPayConfig {

    // ⚠️ BẠN CẦN CUNG CẤP: Mã website của bạn tại VNPay (đăng ký tại sandbox.vnpayment.vn)
    @Value("${vnpay.tmn-code:DEMO}")  // TODO: Thay bằng mã TMN_CODE thật từ VNPay
    private String tmnCode;

    // ⚠️ BẠN CẦN CUNG CẤP: Secret key để mã hóa (lấy từ VNPay dashboard)
    @Value("${vnpay.hash-secret:DEMOSECRETKEY}")  // TODO: Thay bằng HASH_SECRET thật
    private String hashSecret;

    // ⚠️ BẠN CẦN CUNG CẤP: URL API của VNPay
    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpPayUrl;

    // ⚠️ BẠN CẦN CUNG CẤP: URL return về hệ thống sau khi thanh toán (frontend hoặc backend)
    @Value("${vnpay.return-url:http://localhost:8080/api/payment/vnpay-return}")
    private String returnUrl;

    // API Version của VNPay
    @Value("${vnpay.version:2.1.0}")
    private String version;

    // Command mặc định
    @Value("${vnpay.command:pay}")
    private String command;

    // Currency code (VND)
    @Value("${vnpay.currency-code:VND}")
    private String currencyCode;

    // Locale (vn hoặc en)
    @Value("${vnpay.locale:vn}")
    private String locale;

    /**
     * HƯỚNG DẪN ĐĂNG KÝ VNPay SANDBOX:
     * 1. Truy cập: https://sandbox.vnpayment.vn/devreg
     * 2. Đăng ký tài khoản merchant
     * 3. Lấy thông tin:
     *    - TMN Code (Mã website)
     *    - Hash Secret (Secret Key)
     * 4. Cập nhật vào file application.properties:
     *    vnpay.tmn-code=YOUR_TMN_CODE
     *    vnpay.hash-secret=YOUR_HASH_SECRET
     *    vnpay.return-url=http://your-domain.com/api/payment/vnpay-return
     */
}
