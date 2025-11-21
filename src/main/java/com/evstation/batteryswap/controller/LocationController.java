package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.StationLocationRequest;
import com.evstation.batteryswap.dto.response.NearbyStationResponse;
import com.evstation.batteryswap.exception.StationNotFoundException;
import com.evstation.batteryswap.repository.LocationRepository;
import com.evstation.batteryswap.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final LocationRepository locationRepository;

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyStationResponse>> getNearbyStations(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            @RequestParam(value = "radius", required = false) Double radius,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        StationLocationRequest request = StationLocationRequest.builder()
                .latitude(latitude)
                .longitude(longitude)
                .radiusKm(radius != null ? radius : (radiusKm != null ? radiusKm : 10.0))
                .limit(limit == null ? 10 : limit)
                .build();
        List<NearbyStationResponse> result = locationService.findNearbyStations(request);
        if (result.isEmpty()) {
            throw new StationNotFoundException("No station found within radius");
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/nearest")
    public ResponseEntity<NearbyStationResponse> getNearestStation(
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude
    ) {
        return locationService.findNearestStation(latitude, longitude)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new StationNotFoundException("No station found"));
    }

    @GetMapping("/distance/{stationId}")
    public ResponseEntity<Map<String, Object>> getDistanceToStation(
            @PathVariable("stationId") Long stationId,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude
    ) {
        double distance = locationService.calculateDistanceToStation(latitude, longitude, stationId);
        Map<String, Object> body = new HashMap<>();
        body.put("stationId", stationId);
        body.put("stationName", locationRepository.findStationWithCoordinatesById(stationId)
                .map(s -> s.getName()).orElse(null));
        body.put("distance", distance);
        body.put("unit", "km");
        return ResponseEntity.ok(body);
    }
}


