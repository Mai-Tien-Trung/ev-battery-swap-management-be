package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.entity.Vehicle;
import com.evstation.batteryswap.enums.BatteryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Lấy pin theo vehicleId
    List<BatterySerial> findByVehicleId(Long vehicleId);

    // Lấy tất cả pin theo trạm
    List<BatterySerial> findByStation(Station station);
    
    @Query(value = "SELECT * FROM battery_serials WHERE station_id = :stationId AND status = 'AVAILABLE' ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    // 🔹 Tìm pin ngẫu nhiên trong trạm (bạn đã có)
    Optional<BatterySerial> findRandomAvailableBatteryAtStation(Long stationId);

    // 🔹 Thêm method mới để confirm lấy đúng pin đang chờ xác nhận (PENDING_IN)
    Optional<BatterySerial> findFirstByStationIdAndStatus(Long stationId, BatteryStatus status);


    // Lấy pin theo status và không có vehicle (chờ activation)
    List<BatterySerial> findByStatusAndVehicleIsNull(BatteryStatus status);

}
