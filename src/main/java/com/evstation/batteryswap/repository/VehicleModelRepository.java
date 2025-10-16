package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    boolean existsByName(String name);
}
