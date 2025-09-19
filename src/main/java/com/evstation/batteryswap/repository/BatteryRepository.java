package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Battery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatteryRepository extends JpaRepository<Battery, Long> {
    boolean existsBySerialNumber(String serialNumber);
}
