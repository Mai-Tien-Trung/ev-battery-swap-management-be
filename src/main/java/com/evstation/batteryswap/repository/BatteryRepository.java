package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Battery;
import com.evstation.batteryswap.enums.BatteryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatteryRepository extends JpaRepository<Battery, Long> {
    boolean existsBySerialNumber(String serialNumber);

    long countByStation_IdAndStatus(Long stationId, BatteryStatus status);
}
