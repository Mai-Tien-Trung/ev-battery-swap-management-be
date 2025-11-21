package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.PayInvoiceRequestDTO;
import com.evstation.batteryswap.dto.response.InvoicePaymentResponse;
import com.evstation.batteryswap.dto.response.InvoiceResponse;
import com.evstation.batteryswap.dto.response.PendingInvoiceCheckResponse;
import com.evstation.batteryswap.entity.Invoice;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Lấy danh sách tất cả invoice của user
     */
    @GetMapping
    
    public ResponseEntity<List<InvoiceResponse>> getUserInvoices(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        List<InvoiceResponse> invoices = invoiceService.getUserInvoicesDTO(userId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Lấy chi tiết một invoice cụ thể của user
     * GET /api/user/invoices/{invoiceId}
     * 
     * User chỉ có thể xem invoice của chính mình
     */
    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        InvoiceResponse invoice = invoiceService.getInvoiceByIdForUser(invoiceId, userId);
        return ResponseEntity.ok(invoice);
    }

    /**
     * Lấy invoice pending của một subscription
     */
    @GetMapping("/pending/{subscriptionId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<InvoiceResponse>> getPendingInvoices(
            @PathVariable Long subscriptionId
    ) {
        List<InvoiceResponse> invoices = invoiceService.getPendingInvoicesDTO(subscriptionId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Thanh toán invoice thủ công (DEPRECATED - chỉ dùng cho testing)
     * 
     * ⚠️ KHUYẾN NGHỊ: Sử dụng VNPay payment thay vì endpoint này
     * Gọi POST /api/payment/create-vnpay-url để tạo URL thanh toán VNPay
     */
    @Deprecated
    @PostMapping("/{invoiceId}/pay-manual")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<InvoicePaymentResponse> payInvoiceManual(@PathVariable Long invoiceId) {
        Invoice invoice = invoiceService.markAsPaid(invoiceId);
        
        InvoicePaymentResponse response = InvoicePaymentResponse.builder()
                .success(true)
                .message("Invoice paid successfully (MANUAL - FOR TESTING ONLY)")
                .invoiceId(invoice.getId())
                .amount(invoice.getAmount())
                .status(invoice.getStatus())
                .paidAt(invoice.getPaidAt())
                .warning("Use VNPay payment in production")
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra subscription có invoice pending không
     */
    @GetMapping("/check-pending/{subscriptionId}")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<PendingInvoiceCheckResponse> checkPendingInvoices(@PathVariable Long subscriptionId) {
        boolean hasPending = invoiceService.hasPendingInvoices(subscriptionId);
        List<InvoiceResponse> pendingInvoices = invoiceService.getPendingInvoicesDTO(subscriptionId);
        
        double totalPendingAmount = pendingInvoices.stream()
                .mapToDouble(InvoiceResponse::getAmount)
                .sum();
        
        PendingInvoiceCheckResponse response = PendingInvoiceCheckResponse.builder()
                .subscriptionId(subscriptionId)
                .hasPendingInvoices(hasPending)
                .pendingCount(pendingInvoices.size())
                .totalPendingAmount(totalPendingAmount)
                .build();
        
        return ResponseEntity.ok(response);
    }
}
