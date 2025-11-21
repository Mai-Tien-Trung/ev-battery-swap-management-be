package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.PaymentRequestDTO;
import com.evstation.batteryswap.dto.response.PaymentResponse;
import com.evstation.batteryswap.dto.response.VNPayCallbackResult;
import com.evstation.batteryswap.entity.Invoice;
import com.evstation.batteryswap.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final VNPayService vnPayService;

    /**
     * Tạo URL thanh toán VNPay cho invoice
     * 
     * Request body:
     * {
     *   "invoiceId": 5
     * }
     * 
     * Response:
     * {
     *   "success": true,
     *   "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
     *   "message": "Redirect user to this URL to complete payment"
     * }
     */
    @PostMapping("/create-vnpay-url")
    public ResponseEntity<?> createPaymentUrl(
            @RequestBody PaymentRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        try {
            if (request.getInvoiceId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "invoiceId is required"));
            }

            String ipAddress = vnPayService.getIpAddress(httpRequest);
            String paymentUrl = vnPayService.createPaymentUrl(request.getInvoiceId(), ipAddress);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentUrl", paymentUrl,
                    "message", "Redirect user to this URL to complete payment"
            ));

        } catch (Exception e) {
            log.error("Error creating VNPay URL", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * VNPay callback endpoint (vnp_ReturnUrl)
     * 
     * ⚠️ URL này phải được cấu hình trong application.properties:
     *    vnpay.return-url=http://your-domain.com/api/payment/vnpay-return
     * 
     * VNPay sẽ redirect user về đây sau khi thanh toán với các query params:
     * - vnp_TxnRef: Mã giao dịch
     * - vnp_ResponseCode: Mã kết quả (00 = success)
     * - vnp_TransactionNo: Mã GD tại VNPay
     * - vnp_SecureHash: Hash để verify
     * 
     * Flow:
     * 1. User thanh toán trên VNPay
     * 2. VNPay redirect về URL này
     * 3. Backend verify hash và cập nhật Invoice → PAID
     * 4. Redirect user về frontend success page
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<PaymentResponse> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            log.info("VNPAY CALLBACK RECEIVED | params={}", params);

            VNPayCallbackResult callbackResult = vnPayService.processVNPayCallback(params);
            Invoice invoice = callbackResult.getInvoice();

            String message = callbackResult.isSuccess() ? "Payment successful" : "Payment failed";

            PaymentResponse.PaymentResponseBuilder responseBuilder = PaymentResponse.builder()
                    .success(callbackResult.isSuccess())
                    .message(message)
                    .transactionRef(callbackResult.getTransactionRef())
                    .responseCode(callbackResult.getResponseCode())
                    .vnpTransactionNo(callbackResult.getVnpTransactionNo())
                    .bankCode(callbackResult.getBankCode());

            // Thêm thông tin invoice nếu có
            if (invoice != null) {
                responseBuilder
                        .invoiceId(invoice.getId())
                        .amount(invoice.getAmount())
                        .invoiceStatus(invoice.getStatus())
                        .description(invoice.getDescription())
                        .paidAt(invoice.getPaidAt());
            }

            return ResponseEntity.ok(responseBuilder.build());

        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            
            PaymentResponse errorResponse = PaymentResponse.builder()
                    .success(false)
                    .message("Error processing payment: " + e.getMessage())
                    .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }


    @GetMapping("/status/{invoiceId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Long invoiceId) {
        // TODO: Implement nếu cần query status từ InvoiceService
        return ResponseEntity.ok(Map.of(
                "message", "Not implemented yet",
                "invoiceId", invoiceId
        ));
    }
}
