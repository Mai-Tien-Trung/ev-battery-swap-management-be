package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.enums.BatteryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BatterySerialRepository extends JpaRepository<BatterySerial, Long> {
    boolean existsBySerialNumber(String serialNumber);
    // Đếm tổng số pin (trong trạm cụ thể)
    long countByStationId(Long stationId);

    // (không bị maintenance)
    @Query("SELECT COUNT(b) FROM BatterySerial b " +
            "WHERE b.station.id = :stationId " +
            "AND b.status <> com.evstation.batteryswap.enums.BatteryStatus.MAINTENANCE")
    long countActiveBatteriesByStation(Long stationId);
    long countByStationIdAndStatusNot(Long stationId, BatteryStatus status);
}
