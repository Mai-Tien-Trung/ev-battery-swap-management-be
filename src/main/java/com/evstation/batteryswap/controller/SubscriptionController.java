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

    // User yêu cầu đổi gói (không đổi ngay, chỉ set nextPlanId)
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @PutMapping("/{vehicleId}/change-plan")
    public ResponseEntity<?> requestChangePlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long vehicleId,
            @RequestBody ChangePlanRequest request
    ) {
        Subscription updatedSub = subscriptionService.changePlan(
                userDetails.getId(),
                vehicleId,
                request.getNewPlanId()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Change plan request saved. New plan will apply after current subscription ends.",
                        "subscription", Map.of(
                                "id", updatedSub.getId(),
                                "currentPlan", updatedSub.getPlan().getName(),
                                "nextPlanId", updatedSub.getNextPlanId(),
                                "status", updatedSub.getStatus(),
                                "endDate", updatedSub.getEndDate()
                        )
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
