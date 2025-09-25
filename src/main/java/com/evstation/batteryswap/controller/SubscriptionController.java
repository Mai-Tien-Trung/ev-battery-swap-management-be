package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.ChangePlanRequest;
import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
@RestController
@RequestMapping("/api/user/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // User yêu cầu đổi gói (không đổi ngay, chỉ set nextPlanId)
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
}
