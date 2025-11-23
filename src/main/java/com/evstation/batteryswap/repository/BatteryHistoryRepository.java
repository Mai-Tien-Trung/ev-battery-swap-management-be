package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.BatteryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatteryHistoryRepository extends JpaRepository<BatteryHistory, Long> {

    List<BatteryHistory> findByBatterySerialIdOrderByCreatedAtDesc(Long batterySerialId);

    List<BatteryHistory> findByBatterySerialIdAndEventTypeOrderByCreatedAtDesc(
            Long batterySerialId,
            com.evstation.batteryswap.enums.BatteryEventType eventType);
}
