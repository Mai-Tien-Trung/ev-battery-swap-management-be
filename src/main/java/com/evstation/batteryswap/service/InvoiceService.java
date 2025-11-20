package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.response.InvoiceResponse;
import com.evstation.batteryswap.entity.Invoice;
import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.enums.PlanType;

import java.util.List;

public interface InvoiceService {

    /**
     * Tạo invoice khi user vượt base usage (swap overage)
     * @param subscription Subscription liên quan
     * @param swapTransaction Swap transaction gây ra overage
     * @param overage Số lượng vượt (km hoặc kWh)
     * @param rate Tier rate (VND/km hoặc VND/kWh)
     * @param planType DISTANCE hoặc ENERGY
     * @return Invoice đã tạo
     */
    Invoice createInvoice(Subscription subscription, SwapTransaction swapTransaction,
                          double overage, double rate, PlanType planType);

    /**
     * Tạo invoice cho subscription renewal (gia hạn gói)
     * @param subscription Subscription cần gia hạn
     * @param planPrice Giá của gói subscription
     * @param planName Tên gói subscription
     * @return Invoice đã tạo
     */
    Invoice createSubscriptionRenewalInvoice(Subscription subscription, Double planPrice, String planName);

    /**
     * Tạo invoice cho plan change (đổi gói giữa chừng)
     * @param subscription Subscription mới (PENDING)
     * @param planPrice Giá của gói mới
     * @param planName Tên gói mới
     * @return Invoice đã tạo
     */
    Invoice createPlanChangeInvoice(Subscription subscription, Double planPrice, String planName);

    /**
     * Đánh dấu invoice đã thanh toán
     * @param invoiceId ID của invoice
     * @return Invoice đã cập nhật
     */
    Invoice markAsPaid(Long invoiceId);

    /**
     * Kiểm tra subscription có invoice pending không
     * @param subscriptionId ID của subscription
     * @return true nếu có invoice pending
     */
    boolean hasPendingInvoices(Long subscriptionId);

    /**
     * Lấy danh sách invoice pending của subscription
     * @param subscriptionId ID của subscription
     * @return Danh sách invoice pending
     */
    List<Invoice> getPendingInvoices(Long subscriptionId);

    /**
     * Lấy tất cả invoice của user
     * @param userId ID của user
     * @return Danh sách invoice
     */
    List<Invoice> getUserInvoices(Long userId);

    /**
     * Convert Invoice entity sang InvoiceResponse DTO
     * @param invoice Invoice entity
     * @return InvoiceResponse DTO
     */
    InvoiceResponse toResponse(Invoice invoice);

    /**
     * Lấy tất cả invoice của user (DTO)
     * @param userId ID của user
     * @return Danh sách InvoiceResponse
     */
    List<InvoiceResponse> getUserInvoicesDTO(Long userId);

    /**
     * Lấy invoice pending của subscription (DTO)
     * @param subscriptionId ID của subscription
     * @return Danh sách InvoiceResponse
     */
    List<InvoiceResponse> getPendingInvoicesDTO(Long subscriptionId);

    /**
     * Lấy chi tiết invoice theo ID (chỉ nếu invoice thuộc về user)
     * @param invoiceId ID của invoice
     * @param userId ID của user
     * @return InvoiceResponse
     * @throws RuntimeException nếu invoice không tồn tại hoặc không thuộc về user
     */
    InvoiceResponse getInvoiceByIdForUser(Long invoiceId, Long userId);
}
