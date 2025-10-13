package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.StationLocationRequest;
import com.evstation.batteryswap.dto.response.NearbyStationResponse;

import java.util.List;
import java.util.Optional;

public interface LocationService {
    double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2);

    List<NearbyStationResponse> findNearbyStations(StationLocationRequest request);

    Optional<NearbyStationResponse> findNearestStation(Double lat, Double lon);

    double calculateDistanceToStation(Double userLat, Double userLon, Long stationId);

    void validateCoordinates(Double lat, Double lon);
}


