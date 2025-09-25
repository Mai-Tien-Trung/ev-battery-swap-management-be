package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping("/auto-renew")
    public ResponseEntity<?> runAutoRenew() {
        subscriptionService.autoRenewSubscriptions();
        return ResponseEntity.ok(Map.of("message", "Auto renew executed manually"));
    }
}
