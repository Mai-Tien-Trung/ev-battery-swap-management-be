package com.evstation.batteryswap.controller;



import com.evstation.batteryswap.dto.request.PlanTierRateRequest;
import com.evstation.batteryswap.dto.response.PlanTierRateResponse;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.service.PlanTierRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/plan-tiers")
@RequiredArgsConstructor
public class PlanTierRateController {

    private final PlanTierRateService planTierRateService;

    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<PlanTierRateResponse>> getAll() {
        return ResponseEntity.ok(planTierRateService.getAll());
    }

    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @GetMapping("/type/{planType}")
    public ResponseEntity<List<PlanTierRateResponse>> getByPlanType(@PathVariable PlanType planType) {
        return ResponseEntity.ok(planTierRateService.getByPlanType(planType));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<PlanTierRateResponse> create(@RequestBody PlanTierRateRequest req) {
        return ResponseEntity.ok(planTierRateService.create(req));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<PlanTierRateResponse> update(@PathVariable Long id, @RequestBody PlanTierRateRequest req) {
        return ResponseEntity.ok(planTierRateService.update(id, req));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        planTierRateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
