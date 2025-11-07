package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.Vehicle;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Helper API để debug và kiểm tra trạng thái battery/vehicle
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {

    private final VehicleRepository vehicleRepository;
    private final BatterySerialRepository batterySerialRepository;

    /**
     * Lấy thông tin vehicle và các battery đang gắn
     * GET /api/debug/vehicle/4
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<?> getVehicleInfo(@PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Tìm tất cả battery đang gắn vào vehicle này
        List<BatterySerial> batteries = batterySerialRepository.findAll().stream()
                .filter(b -> b.getVehicle() != null && b.getVehicle().getId().equals(vehicleId))
                .toList();

        return ResponseEntity.ok(Map.of(
                "vehicleId", vehicle.getId(),
                "vin", vehicle.getVin(),
                "modelId", vehicle.getModel().getId(),
                "modelName", vehicle.getModel().getName(),
                "efficiencyKmPerKwh", vehicle.getEfficiencyKmPerKwh(),
                "batteriesAttached", batteries.stream().map(b -> Map.of(
                        "id", b.getId(),
                        "serialNumber", b.getSerialNumber(),
                        "status", b.getStatus(),
                        "chargePercent", b.getChargePercent(),
                        "soh", b.getStateOfHealth()
                )).toList()
        ));
    }

    /**
     * Lấy thông tin battery serial
     * GET /api/debug/battery/19
     */
    @GetMapping("/battery/{batterySerialId}")
    public ResponseEntity<?> getBatteryInfo(@PathVariable Long batterySerialId) {
        BatterySerial battery = batterySerialRepository.findById(batterySerialId)
                .orElseThrow(() -> new RuntimeException("Battery serial not found"));

        return ResponseEntity.ok(Map.of(
                "id", battery.getId(),
                "serialNumber", battery.getSerialNumber(),
                "status", battery.getStatus(),
                "chargePercent", battery.getChargePercent() != null ? battery.getChargePercent() : "null",
                "soh", battery.getStateOfHealth(),
                "vehicleId", battery.getVehicle() != null ? battery.getVehicle().getId() : "null",
                "stationId", battery.getStation() != null ? battery.getStation().getId() : "null",
                "batteryType", battery.getBattery().getName()
        ));
    }

    /**
     * Lấy tất cả battery AVAILABLE tại station
     * GET /api/debug/station/1/batteries
     */
    @GetMapping("/station/{stationId}/batteries")
    public ResponseEntity<?> getStationBatteries(@PathVariable Long stationId) {
        List<BatterySerial> batteries = batterySerialRepository.findAll().stream()
                .filter(b -> b.getStation() != null && b.getStation().getId().equals(stationId))
                .toList();

        return ResponseEntity.ok(Map.of(
                "stationId", stationId,
                "totalBatteries", batteries.size(),
                "batteries", batteries.stream().map(b -> Map.of(
                        "id", b.getId(),
                        "serialNumber", b.getSerialNumber(),
                        "status", b.getStatus(),
                        "chargePercent", b.getChargePercent(),
                        "soh", b.getStateOfHealth()
                )).toList()
        ));
    }
}
