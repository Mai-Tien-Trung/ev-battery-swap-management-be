package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.config.VNPayConfig;
import com.evstation.batteryswap.dto.response.VNPayCallbackResult;
import com.evstation.batteryswap.entity.Invoice;
import com.evstation.batteryswap.entity.PaymentTransaction;
import com.evstation.batteryswap.enums.InvoiceStatus;
import com.evstation.batteryswap.enums.PaymentStatus;
import com.evstation.batteryswap.repository.InvoiceRepository;
import com.evstation.batteryswap.repository.PaymentTransactionRepository;
import com.evstation.batteryswap.service.SubscriptionService;
import com.evstation.batteryswap.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayServiceImpl implements VNPayService {

    private final VNPayConfig vnPayConfig;
    private final InvoiceRepository invoiceRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionService subscriptionService;

    @Override
    public String createPaymentUrl(Long invoiceId, String ipAddress) {
        // 1. Lấy invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Invoice already paid");
        }

        // 2. Tạo mã giao dịch unique (vnp_TxnRef)
        String txnRef = "INV" + invoiceId + "_" + System.currentTimeMillis();

        // 3. Số tiền (VNPay yêu cầu nhân 100, không dấu phẩy)
        long amount = (long) (invoice.getAmount() * 100);

        // 4. Tạo payment transaction record
        PaymentTransaction transaction = PaymentTransaction.builder()
                .invoice(invoice)
                .transactionRef(txnRef)
                .amount(invoice.getAmount())
                .status(PaymentStatus.PENDING)
                .orderInfo("Thanh toan hoa don " + invoiceId)
                .createdAt(LocalDateTime.now())
                .build();
        paymentTransactionRepository.save(transaction);

        // 5. Build VNPay parameters
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan hoa don " + invoiceId);
        vnpParams.put("vnp_OrderType", "billpayment");
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);

        // Thời gian tạo và hết hạn
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        calendar.add(Calendar.MINUTE, 15); // Hết hạn sau 15 phút
        String vnpExpireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // 6. Sắp xếp params và tạo hash
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding field: {}", fieldName, e);
                }

                // Build query string
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnpSecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        String paymentUrl = vnPayConfig.getVnpPayUrl() + "?" + queryUrl;

        log.info("VNPAY URL CREATED | invoiceId={} | txnRef={} | amount={} | url={}", 
                invoiceId, txnRef, invoice.getAmount(), paymentUrl);

        return paymentUrl;
    }

    @Override
    @Transactional
    public VNPayCallbackResult processVNPayCallback(Map<String, String> params) {
        // 1. Lấy secure hash từ VNPay
        String vnpSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        params.remove("vnp_SecureHash");

        // 2. Tạo hash từ params để verify
        String signValue = hashAllFields(params);

        // 3. Verify signature
        if (!signValue.equals(vnpSecureHash)) {
            log.error("VNPAY CALLBACK | Invalid signature | expected={} | actual={}", 
                    signValue, vnpSecureHash);
            return VNPayCallbackResult.builder()
                    .success(false)
                    .transactionRef(params.get("vnp_TxnRef"))
                    .responseCode(params.get("vnp_ResponseCode"))
                    .vnpTransactionNo(params.get("vnp_TransactionNo"))
                    .bankCode(params.get("vnp_BankCode"))
                    .build();
        }

        // 4. Lấy thông tin giao dịch
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");

        // 5. Tìm payment transaction
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + txnRef));

        // 6. Cập nhật transaction
        transaction.setResponseCode(responseCode);
        transaction.setVnpTransactionNo(vnpTransactionNo);
        transaction.setBankCode(bankCode);
        transaction.setSecureHash(vnpSecureHash);
        transaction.setPaidAt(LocalDateTime.now());

        // 7. Kiểm tra response code (00 = success)
        if ("00".equals(responseCode)) {
            transaction.setStatus(PaymentStatus.SUCCESS);

            // ✅ Cập nhật Invoice sang PAID
            Invoice invoice = transaction.getInvoice();
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
            invoiceRepository.save(invoice);

            log.info("VNPAY PAYMENT SUCCESS | invoiceId={} | txnRef={} | amount={} | vnpTxnNo={}", 
                    invoice.getId(), txnRef, transaction.getAmount(), vnpTransactionNo);

            // 🔄 Xử lý theo loại invoice
            if ("SUBSCRIPTION_RENEWAL".equals(invoice.getInvoiceType())) {
                // Renewal: complete renewal (tạo subscription mới)
                try {
                    subscriptionService.completeRenewal(invoice.getSubscription().getId());
                    log.info("SUBSCRIPTION RENEWED | subscriptionId={} | invoiceId={} | amount={}",
                            invoice.getSubscription().getId(), invoice.getId(), invoice.getAmount());
                } catch (Exception e) {
                    log.error("Failed to complete subscription renewal after payment | subscriptionId={} | invoiceId={}",
                            invoice.getSubscription().getId(), invoice.getId(), e);
                    // Payment đã thành công nhưng renewal failed - cần manual intervention
                }
            } else if (invoice.getSubscription() != null 
                    && invoice.getSubscription().getStatus() == com.evstation.batteryswap.enums.SubscriptionStatus.PENDING) {
                // Initial subscription payment: activate subscription
                try {
                    subscriptionService.activateSubscription(invoice.getSubscription().getId());
                    log.info("SUBSCRIPTION ACTIVATED | subscriptionId={} | invoiceId={} | amount={}",
                            invoice.getSubscription().getId(), invoice.getId(), invoice.getAmount());
                } catch (Exception e) {
                    log.error("Failed to activate subscription after payment | subscriptionId={} | invoiceId={}",
                            invoice.getSubscription().getId(), invoice.getId(), e);
                    // Payment đã thành công nhưng activation failed - cần manual intervention
                }
            }

            paymentTransactionRepository.save(transaction);
            
            return VNPayCallbackResult.builder()
                    .success(true)
                    .invoice(invoice)
                    .transactionRef(txnRef)
                    .responseCode(responseCode)
                    .vnpTransactionNo(vnpTransactionNo)
                    .bankCode(bankCode)
                    .build();

        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            paymentTransactionRepository.save(transaction);

            log.warn("VNPAY PAYMENT FAILED | txnRef={} | responseCode={} | message={}", 
                    txnRef, responseCode, getResponseMessage(responseCode));
            
            return VNPayCallbackResult.builder()
                    .success(false)
                    .invoice(transaction.getInvoice())
                    .transactionRef(txnRef)
                    .responseCode(responseCode)
                    .vnpTransactionNo(vnpTransactionNo)
                    .bankCode(bankCode)
                    .build();
        }
    }

    @Override
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    // ============ UTILITY METHODS ============

    /**
     * Tạo HMAC SHA512 hash
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error creating HMAC SHA512", e);
            return "";
        }
    }

    /**
     * Hash tất cả fields để verify callback
     */
    private String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                try {
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    log.error("Error encoding field: {}", fieldName, e);
                }
                if (itr.hasNext()) {
                    sb.append("&");
                }
            }
        }
        return hmacSHA512(vnPayConfig.getHashSecret(), sb.toString());
    }

    /**
     * Map VNPay response code sang message
     */
    private String getResponseMessage(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Giao dịch không thành công do: Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10" -> "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản bị khóa";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản không đủ số dư";
            case "65" -> "Giao dịch không thành công do: Tài khoản đã vượt quá hạn mức giao dịch trong ngày";
            default -> "Giao dịch thất bại";
        };
    }
}
