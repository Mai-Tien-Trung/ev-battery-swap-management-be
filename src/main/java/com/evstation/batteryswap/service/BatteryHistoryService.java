package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.response.BatteryHistoryResponse;
import com.evstation.batteryswap.dto.response.BatteryHistoryWithCountResponse;
import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.entity.Vehicle;
import com.evstation.batteryswap.enums.BatteryEventType;

import java.util.List;

public interface BatteryHistoryService {

    /**
     * Log a battery event
     */
    void logEvent(BatterySerial battery, BatteryEventType eventType,
            String oldValue, String newValue,
            Station station, Vehicle vehicle, User performedBy, String notes);

    /**
     * Get history for a specific battery
     */
    List<BatteryHistoryResponse> getBatteryHistory(Long batterySerialId);

    /**
     * Get history for a specific battery filtered by event type
     */
    List<BatteryHistoryResponse> getBatteryHistoryByEventType(Long batterySerialId, BatteryEventType eventType);

    /**
     * Get history with swap count summary
     */
    BatteryHistoryWithCountResponse getBatteryHistoryWithCount(Long batterySerialId);
}
