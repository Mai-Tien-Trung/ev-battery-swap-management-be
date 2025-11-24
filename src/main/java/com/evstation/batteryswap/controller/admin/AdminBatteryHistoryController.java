package com.evstation.batteryswap.controller.admin;

import com.evstation.batteryswap.dto.response.BatteryHistoryWithCountResponse;
import com.evstation.batteryswap.service.BatteryHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/batteries")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin Battery History", description = "Admin endpoints for viewing battery history")
public class AdminBatteryHistoryController {

    private final BatteryHistoryService batteryHistoryService;

    @GetMapping("/{batteryId}/history")
    @Operation(summary = "Get battery history with swap count", description = "View complete history of a battery including all events and total swap count")
    public ResponseEntity<BatteryHistoryWithCountResponse> getBatteryHistory(
            @PathVariable Long batteryId) {

        BatteryHistoryWithCountResponse response = batteryHistoryService.getBatteryHistoryWithCount(batteryId);
        return ResponseEntity.ok(response);
    }
}
