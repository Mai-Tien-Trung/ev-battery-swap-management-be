package com.evstation.batteryswap.controller.admin;

import com.evstation.batteryswap.dto.request.BatteryTransferRequest;
import com.evstation.batteryswap.dto.response.BatteryTransferResponse;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.BatteryTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/batteries")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin Battery Transfer", description = "Admin endpoints for transferring batteries between stations")
public class BatteryTransferController {

    private final BatteryTransferService batteryTransferService;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer battery between stations", description = "Transfer a battery from one station to another. Battery must be AVAILABLE.")
    public ResponseEntity<BatteryTransferResponse> transferBattery(
            @RequestBody BatteryTransferRequest request,
            @AuthenticationPrincipal CustomUserDetails admin) {

        BatteryTransferResponse response = batteryTransferService.transferBattery(request, admin.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent-updates")
    @Operation(summary = "Get recently updated batteries", description = "View batteries sorted by recent updates (transfers, status changes, etc.)")
    public ResponseEntity<List<com.evstation.batteryswap.dto.response.BatterySerialResponse>> getRecentlyUpdatedBatteries(
            @RequestParam(required = false) Long stationId,
            @RequestParam(defaultValue = "20") int limit) {

        List<com.evstation.batteryswap.dto.response.BatterySerialResponse> batteries = batteryTransferService
                .getRecentlyUpdatedBatteries(stationId, limit);
        return ResponseEntity.ok(batteries);
    }
}
