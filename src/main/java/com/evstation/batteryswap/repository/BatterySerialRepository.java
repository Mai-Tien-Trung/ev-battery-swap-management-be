package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.entity.Vehicle;
import com.evstation.batteryswap.enums.BatteryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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
    long countByStationIdAndStatus(Long stationId, BatteryStatus status);
    Optional<BatterySerial> findFirstByVehicleAndStatus(Vehicle vehicle, BatteryStatus status);

    //  Lấy toàn bộ pin đang IN_USE của xe (nếu xe có nhiều pin)
    List<BatterySerial> findByVehicleAndStatus(Vehicle vehicle, BatteryStatus status);

    //  Lấy 1 pin có sẵn trong trạm (AVAILABLE)
    Optional<BatterySerial> findFirstByStationAndStatus(Station station, BatteryStatus status);

}
