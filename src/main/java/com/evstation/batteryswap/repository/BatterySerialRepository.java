package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.BatterySerial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatterySerialRepository extends JpaRepository<BatterySerial, Long> {
    boolean existsBySerialNumber(String serialNumber);
}
