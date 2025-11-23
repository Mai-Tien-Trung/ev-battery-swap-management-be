package com.evstation.batteryswap.controller.admin;

import com.evstation.batteryswap.dto.response.BatteryHistoryResponse;
import com.evstation.batteryswap.service.BatteryHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/batteries")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin Battery History", description = "Admin endpoints for viewing battery history")
public class AdminBatteryHistoryController {

    private final BatteryHistoryService batteryHistoryService;

    @GetMapping("/{batteryId}/history")
    @Operation(summary = "Get battery history", description = "View complete history of a battery including all events")
    public ResponseEntity<List<BatteryHistoryResponse>> getBatteryHistory(
            @PathVariable Long batteryId) {

        List<BatteryHistoryResponse> history = batteryHistoryService.getBatteryHistory(batteryId);
        return ResponseEntity.ok(history);
    }
}
