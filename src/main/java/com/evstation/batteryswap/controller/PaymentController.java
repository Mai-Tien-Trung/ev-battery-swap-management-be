package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.entity.Payment;
import com.evstation.batteryswap.entity.Payment.PaymentStatus;
import com.evstation.batteryswap.service.BatterySwapService;
import com.evstation.batteryswap.utils.VnpayConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.evstation.batteryswap.dto.request.PaymentInitRequest;
import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;


    /**
     * Controller for VNPAY payment integration
     * Handles payment initiation and callback processing
     */
    @RestController
    @RequestMapping("/api/payment")
    public class PaymentController {

        private final VnpayConfig vnpayConfig;
        private final BatterySwapService batterySwapService;
        private final Gson gson;

        @Autowired
        public PaymentController(VnpayConfig vnpayConfig, BatterySwapService batterySwapService) {
            this.vnpayConfig = vnpayConfig;
            this.batterySwapService = batterySwapService;
            this.gson = new Gson();
        }

        /**
         * Initialize VNPAY payment
         * POST /api/payment/init
         * @param requestDto DTO containing payment details
         * @param request HTTP request for client IP
         */
        @PostMapping("/init")
        public ResponseEntity<String> initPayment(@Valid @RequestBody PaymentInitRequest requestDto, // Cập nhật tại đây
                                                  HttpServletRequest request) {
            try {
                // Lấy dữ liệu từ DTO
                Long amount = requestDto.getAmount();
                String bankCode = requestDto.getBankCode();
                String language = requestDto.getLanguage();
                String orderInfo = requestDto.getOrderInfo();

                // Validation đã được xử lý bởi @Valid và @NotNull/@Min trong DTO.
                // if (amount == null || amount <= 0) {
                //     return createErrorResponse("Invalid amount");
                // }

                // Get client IP
                String ipAddress = vnpayConfig.getIpAddress(request);

                // Initiate payment in database
                String txnRef = batterySwapService.initiatePayment(
                        amount,
                        orderInfo,
                        bankCode,
                        language,
                        ipAddress
                );

                // Build VNPAY parameters
                Map<String, String> vnpParams = new HashMap<>();
                vnpParams.put("vnp_Version", vnpayConfig.getVnpVersion());
                vnpParams.put("vnp_Command", vnpayConfig.getVnpCommand());
                vnpParams.put("vnp_TmnCode", vnpayConfig.getVnpTmnCode());
                vnpParams.put("vnp_Amount", String.valueOf(amount * 100)); // VNPAY uses VND * 100
                vnpParams.put("vnp_CurrCode", vnpayConfig.getCurrencyCode());
                vnpParams.put("vnp_TxnRef", txnRef);
                vnpParams.put("vnp_OrderInfo", orderInfo != null ? orderInfo : "Thanh toan dich vu sac pin:" + txnRef);
                vnpParams.put("vnp_OrderType", vnpayConfig.getOrderType());
                vnpParams.put("vnp_Locale", language != null ? language : vnpayConfig.getDefaultLocale());
                vnpParams.put("vnp_ReturnUrl", vnpayConfig.getVnpReturnUrl());
                vnpParams.put("vnp_IpAddr", ipAddress);

                // Add bank code if provided
                if (bankCode != null && !bankCode.isEmpty()) {
                    vnpParams.put("vnp_BankCode", bankCode);
                }

                // Add timestamps
                Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                String vnpCreateDate = formatter.format(cld.getTime());
                vnpParams.put("vnp_CreateDate", vnpCreateDate);

                cld.add(Calendar.MINUTE, 15);
                String vnpExpireDate = formatter.format(cld.getTime());
                vnpParams.put("vnp_ExpireDate", vnpExpireDate);

                // Build payment URL
                String paymentUrl = vnpayConfig.buildPaymentUrl(vnpParams);

                // Return success response
                JsonObject response = new JsonObject();
                response.addProperty("code", "00");
                response.addProperty("message", "success");
                response.addProperty("data", paymentUrl);

                return ResponseEntity.ok(gson.toJson(response));

            } catch (Exception e) {
                return createErrorResponse("Payment initialization failed: " + e.getMessage());
            }
        }

    /**
     * VNPAY callback handler
     * GET /api/payment/vnpay-return
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<String> vnpayReturn(HttpServletRequest request) {
        try {
            // Get all query parameters
            Map<String, String> params = new HashMap<>();
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                params.put(paramName, request.getParameter(paramName));
            }

            // Verify signature
            if (!vnpayConfig.verifySignature(new HashMap<>(params))) {
                return createErrorResponse("Invalid signature");
            }

            // Extract parameters
            String txnRef = params.get("vnp_TxnRef");
            String vnpResponseCode = params.get("vnp_ResponseCode");
            String vnpTransactionId = params.get("vnp_TransactionNo");

            if (txnRef == null) {
                return createErrorResponse("Missing transaction reference");
            }

            // Get payment from database
            batterySwapService.getPaymentByTxnRef(txnRef);

            // Update payment status based on response code
            PaymentStatus status;
            if ("00".equals(vnpResponseCode)) {
                status = PaymentStatus.SUCCESS;
            } else {
                status = PaymentStatus.FAILED;
            }

            // Update payment in database
            batterySwapService.updatePaymentStatus(txnRef, status, vnpTransactionId, vnpResponseCode);

            // Return response
            JsonObject response = new JsonObject();
            response.addProperty("code", vnpResponseCode);
            response.addProperty("message", "00".equals(vnpResponseCode) ? "Payment successful" : "Payment failed");
            response.addProperty("txnRef", txnRef);
            response.addProperty("status", status.name());

            return ResponseEntity.ok(gson.toJson(response));

        } catch (Exception e) {
            return createErrorResponse("Callback processing failed: " + e.getMessage());
        }
    }

    /**
     * Get payment status by transaction reference
     * GET /api/payment/status/{txnRef}
     */
    @GetMapping("/status/{txnRef}")
    public ResponseEntity<String> getPaymentStatus(@PathVariable String txnRef) {
        try {
            Payment payment = batterySwapService.getPaymentByTxnRef(txnRef);
            
            JsonObject response = new JsonObject();
            response.addProperty("code", "00");
            response.addProperty("message", "success");
            response.addProperty("txnRef", payment.getTxnRef());
            response.addProperty("amount", payment.getAmount());
            response.addProperty("status", payment.getStatus().name());
            response.addProperty("createdAt", payment.getCreatedAt().toString());

            return ResponseEntity.ok(gson.toJson(response));

        } catch (Exception e) {
            return createErrorResponse("Payment not found: " + e.getMessage());
        }
    }

    /**
     * Create error response
     */
    private ResponseEntity<String> createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("code", "99");
        response.addProperty("message", message);
        response.addProperty("data", "");
        return ResponseEntity.ok(gson.toJson(response));
    }
}
