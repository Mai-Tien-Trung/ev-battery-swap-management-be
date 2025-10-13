package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Station, Long> {

    @Query("SELECT s FROM Station s WHERE s.latitude IS NOT NULL AND s.longitude IS NOT NULL AND s.latitude BETWEEN -90 AND 90 AND s.longitude BETWEEN -180 AND 180 AND s.status = 'ACTIVE'")
    List<Station> findAllActiveStationsWithCoordinates();

    @Query("SELECT s FROM Station s WHERE s.id = :stationId AND s.latitude IS NOT NULL AND s.longitude IS NOT NULL AND s.latitude BETWEEN -90 AND 90 AND s.longitude BETWEEN -180 AND 180")
    Optional<Station> findStationWithCoordinatesById(@Param("stationId") Long stationId);
}


