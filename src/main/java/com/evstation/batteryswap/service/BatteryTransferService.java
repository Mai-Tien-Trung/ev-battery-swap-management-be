package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.BatteryTransferRequest;
import com.evstation.batteryswap.dto.response.BatteryTransferResponse;

import java.util.List;

public interface BatteryTransferService {
    BatteryTransferResponse transferBattery(BatteryTransferRequest request, Long adminUserId);

    List<com.evstation.batteryswap.dto.response.BatterySerialResponse> getRecentlyUpdatedBatteries(Long stationId,
            int limit);
}
