package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.BatteryRequest;
import com.evstation.batteryswap.dto.response.BatteryResponse;
import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.enums.BatteryStatus;

import java.util.List;

public interface BatterySerialService {

    List<BatteryResponse> getAll();

    BatteryResponse getById(Long id);

    BatteryResponse create(BatteryRequest request);

    BatteryResponse update(Long id, BatteryRequest request);

    void delete(Long id);
    BatterySerial updateStatus(Long id, BatteryStatus status);
}
