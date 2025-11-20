package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.response.PlanChangeResponse;
import com.evstation.batteryswap.dto.response.SubscriptionDetailResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.entity.Invoice;

import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.SubscriptionPlanRepository;
import com.evstation.batteryswap.repository.SubscriptionRepository;

import com.evstation.batteryswap.service.SubscriptionService;
import com.evstation.batteryswap.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final InvoiceService invoiceService;
    private final BatterySerialRepository batterySerialRepository;

    @Override
    @Transactional
    public PlanChangeResponse changePlan(Long userId, Long vehicleId, Long newPlanId) {
        // 1. Tìm subscription ACTIVE hiện tại
        Subscription currentSub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(userId, vehicleId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy subscription ACTIVE cho xe này"));

        // 2. Nếu user đổi sang cùng gói -> báo lỗi
        if (currentSub.getPlan().getId().equals(newPlanId)) {
            throw new IllegalStateException("Xe này đã dùng gói này rồi");
        }

        // 3. Kiểm tra có pending invoices không (bao gồm swap overage, renewal, plan change)
        if (invoiceService.hasPendingInvoices(currentSub.getId())) {
            throw new IllegalStateException("Phải thanh toán hết invoice trước khi đổi plan");
        }

        // 4. Lấy plan mới
        SubscriptionPlan newPlan = subscriptionPlanRepository.findById(newPlanId)
                .orElseThrow(() -> new RuntimeException("Plan không tồn tại: " + newPlanId));

        // 5. Tạo SUBSCRIPTION MỚI với status PENDING
        Subscription newSub = new Subscription();
        newSub.setUser(currentSub.getUser());
        newSub.setVehicle(currentSub.getVehicle());
        newSub.setPlan(newPlan);
        newSub.setStatus(SubscriptionStatus.PENDING);  // ⚠️ PENDING cho đến khi thanh toán
        newSub.setStartDate(LocalDate.now());
        newSub.setEndDate(LocalDate.now().plusDays(newPlan.getDurationDays()));
        newSub.setNextPlanId(null);
        newSub.setEnergyUsedThisMonth(0.0);
        newSub.setDistanceUsedThisMonth(0.0);

        Subscription savedNewSub = subscriptionRepository.save(newSub);

        // 6. Tạo INVOICE cho plan change
        Invoice invoice = invoiceService.createPlanChangeInvoice(
                savedNewSub,
                newPlan.getPrice(),
                newPlan.getName()
        );

        log.info("PLAN CHANGE REQUEST | userId={} | vehicleId={} | oldPlan={} | newPlan={} | oldSubId={} | newSubId={} | invoiceId={} | amount={}₫",
                userId, vehicleId, currentSub.getPlan().getName(), newPlan.getName(),
                currentSub.getId(), savedNewSub.getId(), invoice.getId(), newPlan.getPrice());

        // 7. Build response với cả subscription và invoice info
        return PlanChangeResponse.builder()
                .subscriptionId(savedNewSub.getId())
                .status(savedNewSub.getStatus().name())
                .planName(newPlan.getName())
                .startDate(savedNewSub.getStartDate())
                .endDate(savedNewSub.getEndDate())
                .amount(newPlan.getPrice())
                .invoiceId(invoice.getId())
                .invoiceAmount(invoice.getAmount())
                .message("Plan change request created. Please pay the invoice to activate new plan.")
                .note("After payment, your current ACTIVE subscription will be COMPLETED and this new subscription will be ACTIVE")
                .build();
    }

    @Override
    @Transactional
    public void autoRenewSubscriptions() {
        LocalDate today = LocalDate.now();

        List<Subscription> expiredSubs =
                subscriptionRepository.findByStatusAndEndDate(SubscriptionStatus.ACTIVE, today);

        for (Subscription sub : expiredSubs) {
            // ⚠️ Kiểm tra có invoice pending không (bao gồm cả swap overage và renewal invoices)
            if (invoiceService.hasPendingInvoices(sub.getId())) {
                log.warn("RENEW BLOCKED | subscription={} | vehicle={} | reason=PENDING_INVOICES",
                        sub.getId(), sub.getVehicle().getVin());
                // Không gia hạn, bỏ qua subscription này
                // TODO: Có thể suspend subscription hoặc gửi thông báo
                continue;
            }

            // 1. Lưu lại nextPlanId trước
            Long planId = (sub.getNextPlanId() != null)
                    ? sub.getNextPlanId()
                    : sub.getPlan().getId();

            // 2. Lấy plan mới để tính giá
            SubscriptionPlan newPlan = subscriptionPlanRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan không tồn tại: " + planId));

            // 3. Tạo invoice cho subscription renewal
            Invoice renewalInvoice = invoiceService.createSubscriptionRenewalInvoice(
                    sub, 
                    newPlan.getPrice(), 
                    newPlan.getName()
            );

            log.info("RENEWAL INVOICE CREATED | subscription={} | invoice={} | plan={} | amount={}₫",
                    sub.getId(), renewalInvoice.getId(), newPlan.getName(), newPlan.getPrice());

            // 4. BLOCK gia hạn cho đến khi thanh toán
            // User phải thanh toán invoice này trước khi subscription được renew
            log.warn("RENEW BLOCKED | subscription={} | vehicle={} | reason=RENEWAL_PAYMENT_REQUIRED | invoice={}",
                    sub.getId(), sub.getVehicle().getVin(), renewalInvoice.getId());
            
            // ⚠️ KHÔNG tạo subscription mới ngay - chờ thanh toán
            // Sau khi user thanh toán invoice, có thể có webhook/callback để trigger renew
        }

    }
    @Override
    @Transactional
    public Subscription completeRenewal(Long subscriptionId) {
        Subscription oldSub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

        // 1. Kiểm tra còn invoice pending không
        if (invoiceService.hasPendingInvoices(subscriptionId)) {
            throw new RuntimeException("Cannot renew: pending invoices exist for subscription " + subscriptionId);
        }

        // 2. Lấy planId (nếu có nextPlanId thì dùng, không thì dùng plan hiện tại)
        Long planId = (oldSub.getNextPlanId() != null)
                ? oldSub.getNextPlanId()
                : oldSub.getPlan().getId();

        // 3. Đóng subscription cũ
        oldSub.setStatus(SubscriptionStatus.COMPLETED);
        oldSub.setNextPlanId(null);
        subscriptionRepository.save(oldSub);

        // 4. Lấy plan mới
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan không tồn tại: " + planId));

        // 5. Tạo subscription mới
        Subscription newSub = new Subscription();
        newSub.setUser(oldSub.getUser());
        newSub.setVehicle(oldSub.getVehicle());
        newSub.setPlan(plan);
        newSub.setStatus(SubscriptionStatus.ACTIVE);
        newSub.setStartDate(oldSub.getEndDate().plusDays(1));
        newSub.setEndDate(oldSub.getEndDate().plusDays(plan.getDurationDays()));
        newSub.setNextPlanId(null);
        newSub.setEnergyUsedThisMonth(0.0);
        newSub.setDistanceUsedThisMonth(0.0);

        Subscription saved = subscriptionRepository.save(newSub);

        log.info("RENEW COMPLETED | oldSub={} | newSub={} | vehicle={} | plan={} | startDate={} | endDate={}",
                subscriptionId, saved.getId(), saved.getVehicle().getVin(), 
                plan.getName(), saved.getStartDate(), saved.getEndDate());

        return saved;
    }

    @Override
    @Transactional
    public Subscription activateSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

        // 1. Kiểm tra trạng thái
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new RuntimeException("Can only activate PENDING subscriptions. Current status: " + subscription.getStatus());
        }

        // 2. Kiểm tra còn invoice pending không
        if (invoiceService.hasPendingInvoices(subscriptionId)) {
            throw new RuntimeException("Cannot activate: pending invoices exist for subscription " + subscriptionId);
        }

        // 3. Activate subscription
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        Subscription activated = subscriptionRepository.save(subscription);

        // 4. Assign batteries to vehicle (từ AVAILABLE → IN_USE)
        Vehicle vehicle = subscription.getVehicle();
        int maxBatteries = subscription.getPlan().getMaxBatteries();
        
        // Tìm batteries AVAILABLE chưa có vehicle (batteries được tạo lúc linkVehicle)
        List<BatterySerial> availableBatteries = batterySerialRepository
                .findByStatusAndVehicleIsNull(BatteryStatus.AVAILABLE)
                .stream()
                .limit(maxBatteries)
                .toList();

        if (availableBatteries.size() < maxBatteries) {
            log.warn("Not enough batteries available. Required: {}, Found: {}", 
                    maxBatteries, availableBatteries.size());
        }

        // Assign batteries to vehicle
        for (BatterySerial battery : availableBatteries) {
            battery.setVehicle(vehicle);
            battery.setStatus(BatteryStatus.IN_USE);
            batterySerialRepository.save(battery);
        }

        log.info("SUBSCRIPTION ACTIVATED | subscriptionId={} | vehicleId={} | vehicleVin={} | plan={} | batteriesAssigned={}",
                subscriptionId, vehicle.getId(), vehicle.getVin(), 
                subscription.getPlan().getName(), availableBatteries.size());

        return activated;
    }

    @Override
    @Transactional
    public Subscription activatePlanChange(Long newSubscriptionId) {
        // 1. Lấy subscription mới (PENDING)
        Subscription newSub = subscriptionRepository.findById(newSubscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + newSubscriptionId));

        // 2. Kiểm tra trạng thái
        if (newSub.getStatus() != SubscriptionStatus.PENDING) {
            throw new RuntimeException("Can only activate PENDING subscriptions. Current status: " + newSub.getStatus());
        }

        // 3. Kiểm tra còn invoice pending không
        if (invoiceService.hasPendingInvoices(newSubscriptionId)) {
            throw new RuntimeException("Cannot activate: pending invoices exist for subscription " + newSubscriptionId);
        }

        // 4. Tìm subscription cũ (ACTIVE) cùng user và vehicle
        Optional<Subscription> oldSubOpt = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(
                        newSub.getUser().getId(),
                        newSub.getVehicle().getId(),
                        SubscriptionStatus.ACTIVE
                );

        // 5. Đóng subscription cũ nếu tồn tại
        if (oldSubOpt.isPresent()) {
            Subscription oldSub = oldSubOpt.get();
            oldSub.setStatus(SubscriptionStatus.COMPLETED);
            oldSub.setNextPlanId(null);
            subscriptionRepository.save(oldSub);

            log.info("OLD SUBSCRIPTION COMPLETED | oldSubId={} | plan={} | vehicleVin={}",
                    oldSub.getId(), oldSub.getPlan().getName(), oldSub.getVehicle().getVin());
        }

        // 6. Kích hoạt subscription mới
        newSub.setStatus(SubscriptionStatus.ACTIVE);
        Subscription activated = subscriptionRepository.save(newSub);

        // ⚠️ KHÔNG thêm/bớt batteries - giữ nguyên số pin hiện có

        log.info("PLAN CHANGE ACTIVATED | newSubId={} | plan={} | vehicleVin={} | startDate={} | endDate={}",
                activated.getId(), activated.getPlan().getName(), activated.getVehicle().getVin(),
                activated.getStartDate(), activated.getEndDate());

        return activated;
    }

    @Override
    public SubscriptionDetailResponse getSubscriptionDetail(Long userId, Long vehicleId) {
        Subscription sub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(userId, vehicleId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        Vehicle vehicle = sub.getVehicle();

        SubscriptionPlan currentPlan = sub.getPlan();
        SubscriptionPlan nextPlan = sub.getNextPlanId() != null
                ? subscriptionPlanRepository.findById(sub.getNextPlanId())
                .orElseThrow(() -> new RuntimeException("Next plan not found"))
                : currentPlan;

        return new SubscriptionDetailResponse(
                vehicle.getId(),
                vehicle.getModel().getName() + " " + vehicle.getVin(),
                currentPlan.getName(),
                sub.getStartDate(),
                sub.getEndDate(),
                nextPlan.getName()
        );
    }
    @Override
    public List<SubscriptionDetailResponse> getAllActiveSubscriptions(Long userId) {
        List<Subscription> subs = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        return subs.stream().map(sub -> {
            Vehicle vehicle = sub.getVehicle();
            SubscriptionPlan currentPlan = sub.getPlan();
            SubscriptionPlan nextPlan = sub.getNextPlanId() != null
                    ? subscriptionPlanRepository.findById(sub.getNextPlanId())
                    .orElseThrow(() -> new RuntimeException("Next plan not found"))
                    : currentPlan;

            return new SubscriptionDetailResponse(
                    vehicle.getId(),
                    vehicle.getModel().getName() + " " + vehicle.getVin(),
                    currentPlan.getName(),
                    sub.getStartDate(),
                    sub.getEndDate(),
                    nextPlan.getName()
            );
        }).toList();
    }

}
