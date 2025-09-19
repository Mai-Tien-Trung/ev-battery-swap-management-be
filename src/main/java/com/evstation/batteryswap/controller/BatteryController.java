package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.BatteryRequest;
import com.evstation.batteryswap.dto.response.BatteryResponse;
import com.evstation.batteryswap.service.BatteryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batteries")
public class BatteryController {

    private final BatteryService batteryService;

    public BatteryController(BatteryService batteryService) {
        this.batteryService = batteryService;
    }

    @GetMapping
    public ResponseEntity<List<BatteryResponse>> getAllBatteries() {
        return ResponseEntity.ok(batteryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatteryResponse> getBatteryById(@PathVariable Long id) {
        return ResponseEntity.ok(batteryService.getById(id));
    }

    @PostMapping
    public ResponseEntity<BatteryResponse> createBattery(@Valid @RequestBody BatteryRequest request) {
        return ResponseEntity.ok(batteryService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BatteryResponse> updateBattery(
            @PathVariable Long id,
            @Valid @RequestBody BatteryRequest request) {
        return ResponseEntity.ok(batteryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBattery(@PathVariable Long id) {
        batteryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
