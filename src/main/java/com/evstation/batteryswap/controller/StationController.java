package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.StationRequest;
import com.evstation.batteryswap.dto.response.StationResponse;
import com.evstation.batteryswap.service.StationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public ResponseEntity<List<StationResponse>> getAllStations() {
        return ResponseEntity.ok(stationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationResponse> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<StationResponse> createStation(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(stationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StationResponse> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(stationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
