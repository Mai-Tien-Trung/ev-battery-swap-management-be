package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.UpdateBatterySoHRequest;
import com.evstation.batteryswap.dto.response.UpdateBatterySoHResponse;

public interface AdminBatteryService {
    /**
     * Update battery SoH (State of Health)
     * Only admin can call this. Validates that new SoH is within subscription
     * plan's range.
     * 
     * @param batteryId ID of the battery to update
     * @param request   Contains the new SoH value
     * @return Response with old/new SoH and plan constraints
     */
    UpdateBatterySoHResponse updateBatterySoH(Long batteryId, UpdateBatterySoHRequest request);
}
