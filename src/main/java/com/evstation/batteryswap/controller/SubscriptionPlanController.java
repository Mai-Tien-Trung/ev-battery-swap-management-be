package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.SubscriptionPlanRequest;
import com.evstation.batteryswap.dto.response.SubscriptionPlanResponse;
import com.evstation.batteryswap.service.SubscriptionPlanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-plans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService planService;

    public SubscriptionPlanController(SubscriptionPlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllPlans() {
        return ResponseEntity.ok(planService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SubscriptionPlanResponse> createPlan(@Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(planService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionPlanResponse> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody SubscriptionPlanRequest request) {
        return ResponseEntity.ok(planService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        planService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
