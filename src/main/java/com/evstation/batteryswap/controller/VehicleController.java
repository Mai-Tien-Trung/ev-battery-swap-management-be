package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.VehicleRequest;
import com.evstation.batteryswap.dto.response.VehicleResponse;
import com.evstation.batteryswap.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    // 🔹 Tạo xe mới
    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(vehicleService.createVehicle(request));
    }

    // 🔹 Lấy danh sách tất cả xe
    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAll() {
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    // 🔹 Lấy chi tiết 1 xe theo ID
    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    // 🔹 Update xe
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request
    ) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, request));
    }

    // 🔹 Xóa xe
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }
}

