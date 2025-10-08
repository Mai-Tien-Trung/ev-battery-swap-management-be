package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.entity.Battery;
import com.evstation.batteryswap.service.BatteryService;
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
    public ResponseEntity<List<Battery>> getAll() {
        return ResponseEntity.ok(batteryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Battery> getById(@PathVariable Long id) {
        return ResponseEntity.ok(batteryService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Battery> create(@RequestBody Battery battery) {
        return ResponseEntity.ok(batteryService.create(battery));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Battery> update(@PathVariable Long id, @RequestBody Battery battery) {
        return ResponseEntity.ok(batteryService.update(id, battery));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        batteryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
