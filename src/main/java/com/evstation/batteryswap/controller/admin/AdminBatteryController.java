package com.evstation.batteryswap.controller.admin;

import com.evstation.batteryswap.dto.request.UpdateBatterySoHRequest;
import com.evstation.batteryswap.dto.response.UpdateBatterySoHResponse;
import com.evstation.batteryswap.service.AdminBatteryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Controller for battery management
 * Only ADMIN role can access these endpoints
 */
@RestController
@RequestMapping("/api/admin/batteries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBatteryController {

    private final AdminBatteryService adminBatteryService;

    /**
     * Update battery SoH (State of Health)
     * PUT /api/admin/batteries/{batteryId}/soh
     * 
     * Validates that new SoH is within the subscription plan's allowed range.
     * 
     * @param batteryId ID of the battery to update
     * @param request   Contains the new SoH value
     * @return Response with old/new SoH and plan constraints
     */
    @PutMapping("/{batteryId}/soh")
    public ResponseEntity<UpdateBatterySoHResponse> updateBatterySoH(
            @PathVariable Long batteryId,
            @RequestBody UpdateBatterySoHRequest request) {
        UpdateBatterySoHResponse response = adminBatteryService.updateBatterySoH(batteryId, request);
        return ResponseEntity.ok(response);
    }
}
