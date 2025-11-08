package com.evstation.batteryswap.controller.admin;

import com.evstation.batteryswap.dto.response.InvoiceResponse;
import com.evstation.batteryswap.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Controller để quản lý invoices
 * Chỉ ADMIN mới có quyền truy cập
 */
@RestController
@RequestMapping("/api/admin/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InvoiceAdminController {

    private final InvoiceService invoiceService;

    /**
     * Lấy danh sách tất cả invoice của một user cụ thể
     * GET /api/admin/invoices/user/{userId}
     * 
     * @param userId ID của user cần xem invoices
     * @return Danh sách InvoiceResponse
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByUserId(@PathVariable Long userId) {
        List<InvoiceResponse> invoices = invoiceService.getUserInvoicesDTO(userId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Lấy invoice pending của một subscription cụ thể
     * GET /api/admin/invoices/pending/{subscriptionId}
     * 
     * @param subscriptionId ID của subscription
     * @return Danh sách InvoiceResponse pending
     */
    @GetMapping("/pending/{subscriptionId}")
    public ResponseEntity<List<InvoiceResponse>> getPendingInvoicesBySubscription(
            @PathVariable Long subscriptionId
    ) {
        List<InvoiceResponse> invoices = invoiceService.getPendingInvoicesDTO(subscriptionId);
        return ResponseEntity.ok(invoices);
    }
}
