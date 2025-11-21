package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.StationRequest;
import com.evstation.batteryswap.dto.response.StationResponse;
import com.evstation.batteryswap.dto.response.StationSummaryResponse;
import com.evstation.batteryswap.service.StationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<StationResponse>> getAllStations() {
        return ResponseEntity.ok(stationService.getAll());
    }
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<StationResponse> getStationById(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getById(id));
    }
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @PostMapping
    public ResponseEntity<StationResponse> createStation(@Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(stationService.create(request));
    }
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<StationResponse> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody StationRequest request) {
        return ResponseEntity.ok(stationService.update(id, request));
    }
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("hasAuthority('ADMIN')")

    @GetMapping("/{id}/usage")
    public ResponseEntity<String> checkUsage(@PathVariable Long id) {
        stationService.updateStationUsage(id);
        return ResponseEntity.ok("Updated station usage successfully!");
    }
    @PreAuthorize("hasAuthority('ADMIN')")

    @GetMapping("/{id}/summary")
    public ResponseEntity<StationSummaryResponse> getStationSummary(@PathVariable Long id) {
        return ResponseEntity.ok(stationService.getStationSummary(id));
    }
    @PreAuthorize("hasAuthority('ADMIN')")

    @GetMapping("/summary")
    public ResponseEntity<List<StationSummaryResponse>> getAllStationSummaries() {
        return ResponseEntity.ok(stationService.getAllStationSummaries());
    }

}
