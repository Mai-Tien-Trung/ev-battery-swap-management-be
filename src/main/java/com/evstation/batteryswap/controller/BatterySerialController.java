package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.BatteryRequest;
import com.evstation.batteryswap.dto.request.UpdateBatterySerialStatusRequest;
import com.evstation.batteryswap.dto.response.BatteryResponse;
import com.evstation.batteryswap.dto.response.BatterySerialResponse;
import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.service.BatterySerialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/battery-serials")
public class BatterySerialController {

    private final BatterySerialService batterySerialService;

    public BatterySerialController(BatterySerialService batterySerialService) {
        this.batterySerialService = batterySerialService;
    }

    @GetMapping
    public ResponseEntity<List<BatteryResponse>> getAll() {
        return ResponseEntity.ok(batterySerialService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatteryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(batterySerialService.getById(id));
    }

    @PostMapping
    public ResponseEntity<BatteryResponse> create(@Valid @RequestBody BatteryRequest request) {
        return ResponseEntity.ok(batterySerialService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BatteryResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody BatteryRequest request) {
        return ResponseEntity.ok(batterySerialService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        batterySerialService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}/status")
    public ResponseEntity<BatterySerialResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateBatterySerialStatusRequest request
    ) {
        BatterySerial serial = batterySerialService.updateStatus(id, request.getStatus());

        BatterySerialResponse response = BatterySerialResponse.builder()
                .id(serial.getId())
                .serialNumber(serial.getSerialNumber())
                .status(serial.getStatus())
                .stationName(serial.getStation() != null ? serial.getStation().getName() : null)
                .batteryName(serial.getBattery() != null ? serial.getBattery().getName() : null)
                .currentCapacity(serial.getCurrentCapacity())
                .stateOfHealth(serial.getStateOfHealth())
                .build();

        return ResponseEntity.ok(response);
    }


}
