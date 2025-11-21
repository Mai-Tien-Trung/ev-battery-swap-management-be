package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.service.BatterySerialService;
import com.evstation.batteryswap.service.SwapTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
public class AnalyticsController {

    private final SwapTransactionService swapTransactionService;
    private final BatterySerialService batterySerialService;
    @GetMapping("/most-frequent-swap-hour")
    public ResponseEntity<List<Map<String, Object>>> getMostFrequentSwapHour() {
        List<Map<String, Object>> result = swapTransactionService.getMostFrequentSwapHour();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/swaps-per-station")
    public ResponseEntity<List<Map<String, Object>>> getSwapsPerStation() {
        List<Map<String, Object>> result = swapTransactionService.getSwapsPerStation();
        return ResponseEntity.ok(result);
    }
    @GetMapping("/battery-status-distribution")
    public ResponseEntity<List<Map<String, Object>>> getBatteryStatusDistribution() {
        List<Map<String, Object>> result = batterySerialService.getBatteryStatusDistribution();
        return ResponseEntity.ok(result);
    }
}