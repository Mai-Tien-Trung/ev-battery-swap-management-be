package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.ChangePlanRequest;
import com.evstation.batteryswap.dto.response.SubscriptionDetailResponse;
import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/user/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // User yêu cầu đổi gói (tạo subscription mới PENDING + invoice)
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @PutMapping("/{vehicleId}/change-plan")
    public ResponseEntity<?> requestChangePlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long vehicleId,
            @RequestBody ChangePlanRequest request
    ) {
        Subscription newSub = subscriptionService.changePlan(
                userDetails.getId(),
                vehicleId,
                request.getNewPlanId()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Plan change request created. Please pay the invoice to activate new plan.",
                        "newSubscription", Map.of(
                                "id", newSub.getId(),
                                "plan", newSub.getPlan().getName(),
                                "status", newSub.getStatus().name(),
                                "startDate", newSub.getStartDate(),
                                "endDate", newSub.getEndDate(),
                                "amount", newSub.getPlan().getPrice()
                        ),
                        "note", "After payment, your current ACTIVE subscription will be COMPLETED and this new subscription will be ACTIVE"
                )
        );
    }

    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/{vehicleId}")
    public ResponseEntity<SubscriptionDetailResponse> getSubscriptionDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long vehicleId) {
        Long userId = userDetails.getId();
        SubscriptionDetailResponse response = subscriptionService.getSubscriptionDetail(userId, vehicleId);
        return ResponseEntity.ok(response);
    }
    @GetMapping
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<List<SubscriptionDetailResponse>> getAllSubscriptions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        List<SubscriptionDetailResponse> responses = subscriptionService.getAllActiveSubscriptions(userId);
        return ResponseEntity.ok(responses);
    }
}
