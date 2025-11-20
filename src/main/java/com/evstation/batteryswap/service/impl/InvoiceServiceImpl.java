package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.response.InvoiceResponse;
import com.evstation.batteryswap.entity.Invoice;
import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.enums.InvoiceStatus;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.repository.InvoiceRepository;
import com.evstation.batteryswap.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Override
    public Invoice createInvoice(Subscription subscription, SwapTransaction swapTransaction,
                                  double overage, double rate, PlanType planType) {
        
        // Tính tổng tiền
        double amount = overage * rate;

        // Tạo description chi tiết
        String unit = (planType == PlanType.ENERGY) ? "kWh" : "km";
        String description = String.format("Overage: %.2f %s × %.0f₫/%s = %.0f₫",
                overage, unit, rate, unit, amount);

        // Tạo invoice
        Invoice invoice = Invoice.builder()
                .subscription(subscription)
                .swapTransaction(swapTransaction)
                .amount(amount)
                .status(InvoiceStatus.PENDING)
                .invoiceType("SWAP_OVERAGE")
                .usageType(planType)
                .overage(overage)
                .rate(rate)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        
        log.info("INVOICE CREATED | id={} | type=SWAP_OVERAGE | subscription={} | amount={}₫ | overage={} {} | description={}",
                saved.getId(), subscription.getId(), amount, overage, unit, description);

        return saved;
    }

    @Override
    public Invoice createSubscriptionRenewalInvoice(Subscription subscription, Double planPrice, String planName) {
        String description = String.format("Subscription Renewal: %s - %.0f₫", planName, planPrice);

        Invoice invoice = Invoice.builder()
                .subscription(subscription)
                .swapTransaction(null)  // Không có swap transaction
                .amount(planPrice)
                .status(InvoiceStatus.PENDING)
                .invoiceType("SUBSCRIPTION_RENEWAL")
                .usageType(null)  // Không áp dụng cho renewal
                .overage(null)
                .rate(null)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        log.info("INVOICE CREATED | id={} | type=SUBSCRIPTION_RENEWAL | subscription={} | plan={} | amount={}₫",
                saved.getId(), subscription.getId(), planName, planPrice);

        return saved;
    }

    @Override
    public Invoice createPlanChangeInvoice(Subscription subscription, Double planPrice, String planName) {
        String description = String.format("Plan Change: %s - %.0f₫", planName, planPrice);

        Invoice invoice = Invoice.builder()
                .subscription(subscription)  // Link với subscription mới (PENDING)
                .swapTransaction(null)
                .amount(planPrice)
                .status(InvoiceStatus.PENDING)
                .invoiceType("PLAN_CHANGE")
                .usageType(null)
                .overage(null)
                .rate(null)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        Invoice saved = invoiceRepository.save(invoice);

        log.info("INVOICE CREATED | id={} | type=PLAN_CHANGE | subscription={} | plan={} | amount={}₫",
                saved.getId(), subscription.getId(), planName, planPrice);

        return saved;
    }

    @Override
    public Invoice markAsPaid(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            log.warn("Invoice {} is already paid", invoiceId);
            return invoice;
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        
        Invoice updated = invoiceRepository.save(invoice);
        log.info("INVOICE PAID | id={} | amount={}₫ | paidAt={}", 
                invoiceId, invoice.getAmount(), invoice.getPaidAt());

        return updated;
    }

    @Override
    public boolean hasPendingInvoices(Long subscriptionId) {
        return invoiceRepository.hasPendingInvoices(subscriptionId);
    }

    @Override
    public List<Invoice> getPendingInvoices(Long subscriptionId) {
        return invoiceRepository.findPendingBySubscriptionId(subscriptionId);
    }

    @Override
    public List<Invoice> getUserInvoices(Long userId) {
        return invoiceRepository.findByUserId(userId);
    }

    @Override
    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) return null;

        Long invoiceId = invoice.getId();
        Long subscriptionId = invoice.getSubscription() != null ? invoice.getSubscription().getId() : null;
        Long swapTxId = invoice.getSwapTransaction() != null ? invoice.getSwapTransaction().getId() : null;

        String vehicleVin = null;
        String vehicleModel = null;
        String planName = null;
        PlanType planType = null;
        if (invoice.getSubscription() != null) {
            if (invoice.getSubscription().getVehicle() != null) {
                vehicleVin = invoice.getSubscription().getVehicle().getVin();
                if (invoice.getSubscription().getVehicle().getModel() != null)
                    vehicleModel = invoice.getSubscription().getVehicle().getModel().getName();
            }
            if (invoice.getSubscription().getPlan() != null) {
                planName = invoice.getSubscription().getPlan().getName();
                planType = invoice.getSubscription().getPlan().getPlanType();
            }
        }

        String unit = (invoice.getUsageType() == PlanType.ENERGY) ? "kWh" : "km";

        return InvoiceResponse.builder()
                .invoiceId(invoiceId)
                .subscriptionId(subscriptionId)
                .swapTransactionId(swapTxId)
                .vehicleVin(vehicleVin)
                .vehicleModel(vehicleModel)
                .planName(planName)
                .planType(planType)
                .amount(invoice.getAmount())
                .status(invoice.getStatus())
                .description(invoice.getDescription())
                .overage(invoice.getOverage())
                .rate(invoice.getRate())
                .unit(unit)
                .createdAt(invoice.getCreatedAt())
                .paidAt(invoice.getPaidAt())
                .build();
    }

    @Override
    public List<InvoiceResponse> getUserInvoicesDTO(Long userId) {
        return invoiceRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InvoiceResponse> getPendingInvoicesDTO(Long subscriptionId) {
        return invoiceRepository.findPendingBySubscriptionId(subscriptionId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public InvoiceResponse getInvoiceByIdForUser(Long invoiceId, Long userId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        // Kiểm tra invoice có thuộc về user không
        if (invoice.getSubscription() == null || invoice.getSubscription().getUser() == null) {
            throw new RuntimeException("Invoice has no associated user");
        }

        if (!invoice.getSubscription().getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied: Invoice does not belong to this user");
        }

        return toResponse(invoice);
    }
}
