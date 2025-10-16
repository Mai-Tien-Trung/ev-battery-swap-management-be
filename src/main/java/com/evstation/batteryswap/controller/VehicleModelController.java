package com.evstation.batteryswap.controller.admin;

import com.evstation.batteryswap.entity.VehicleModel;
import com.evstation.batteryswap.service.VehicleModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vehicle-models")
@RequiredArgsConstructor
public class VehicleModelController {

    private final VehicleModelService vehicleModelService;

    @PostMapping
    public ResponseEntity<VehicleModel> create(@RequestBody VehicleModel model) {
        return ResponseEntity.ok(vehicleModelService.create(model));
    }

    @GetMapping
    public ResponseEntity<List<VehicleModel>> getAll() {
        return ResponseEntity.ok(vehicleModelService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleModel> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleModelService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleModel> update(@PathVariable Long id, @RequestBody VehicleModel model) {
        return ResponseEntity.ok(vehicleModelService.update(id, model));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleModelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
