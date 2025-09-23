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

    @PutMapping("/{vehicleId}/change-plan")
    public ResponseEntity<?> changePlan(
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
                        "message", "Subscription changed successfully",
                        "newSubscription", Map.of(
                                "id", newSub.getId(),
                                "planName", newSub.getPlan().getName(),
                                "status", newSub.getStatus(),
                                "startDate", newSub.getStartDate()
                        )
                )
        );
    }
}
