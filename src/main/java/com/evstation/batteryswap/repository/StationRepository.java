package com.evstation.batteryswap.repository;


import com.evstation.batteryswap.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station, Long> {
    boolean existsByName(String name);
}
