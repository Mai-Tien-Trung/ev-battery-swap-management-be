package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.StationLocationRequest;
import com.evstation.batteryswap.dto.response.NearbyStationResponse;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.exception.InvalidCoordinatesException;
import com.evstation.batteryswap.exception.StationNotFoundException;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.LocationRepository;
import com.evstation.batteryswap.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final BatterySerialRepository batterySerialRepository;

    @Override
    public double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        validateCoordinates(lat1, lon1);
        validateCoordinates(lat2, lon2);

        GlobalCoordinates user = new GlobalCoordinates(lat1, lon1);
        GlobalCoordinates station = new GlobalCoordinates(lat2, lon2);

        GeodeticCalculator calc = new GeodeticCalculator();
        GeodeticCurve curve = calc.calculateGeodeticCurve(Ellipsoid.WGS84, user, station);
        double meters = curve.getEllipsoidalDistance();
        return meters / 1000.0;
    }

    @Override
    public List<NearbyStationResponse> findNearbyStations(StationLocationRequest request) {
        validateCoordinates(request.getLatitude(), request.getLongitude());

        List<Station> stations = locationRepository.findAllActiveStationsWithCoordinates();

        return stations.stream()
                .map(s -> buildNearbyResponse(request.getLatitude(), request.getLongitude(), s))
                .filter(resp -> resp.getDistance() <= (request.getRadiusKm() == null ? 10.0 : request.getRadiusKm()))
                .sorted(Comparator.comparing(NearbyStationResponse::getDistance))
                .limit(request.getLimit() == null ? 10 : request.getLimit())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NearbyStationResponse> findNearestStation(Double lat, Double lon) {
        validateCoordinates(lat, lon);
        List<Station> stations = locationRepository.findAllActiveStationsWithCoordinates();
        return stations.stream()
                .map(s -> buildNearbyResponse(lat, lon, s))
                .min(Comparator.comparing(NearbyStationResponse::getDistance));
    }

    @Override
    public double calculateDistanceToStation(Double userLat, Double userLon, Long stationId) {
        validateCoordinates(userLat, userLon);
        Station station = locationRepository.findStationWithCoordinatesById(stationId)
                .orElseThrow(() -> new StationNotFoundException("Station not found or missing coordinates"));
        return calculateDistance(userLat, userLon, station.getLatitude(), station.getLongitude());
    }

    @Override
    public void validateCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) {
            throw new InvalidCoordinatesException("Latitude and longitude are required");
        }
        if (lat < -90 || lat > 90) {
            throw new InvalidCoordinatesException("Latitude must be between -90 and 90");
        }
        if (lon < -180 || lon > 180) {
            throw new InvalidCoordinatesException("Longitude must be between -180 and 180");
        }
    }

    private NearbyStationResponse buildNearbyResponse(Double userLat, Double userLon, Station s) {
        double distanceKm = calculateDistance(userLat, userLon, s.getLatitude(), s.getLongitude());
        double rounded = BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP).doubleValue();
    int available = (int) batterySerialRepository.countByStationIdAndStatus(s.getId(), BatteryStatus.AVAILABLE);
        return NearbyStationResponse.builder()
                .stationId(s.getId())
                .stationName(s.getName())
                .stationLocation(s.getLocation())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .distance(rounded)
                .availableBattery(available)
                .phone(s.getPhone())
                .status(s.getStatus().name())
                .build();
    }
}


