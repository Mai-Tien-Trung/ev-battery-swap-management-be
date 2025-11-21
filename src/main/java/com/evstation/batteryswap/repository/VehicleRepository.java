package com.evstation.batteryswap.repository;


import com.evstation.batteryswap.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
