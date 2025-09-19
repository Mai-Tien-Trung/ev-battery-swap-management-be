package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.BatteryRequest;
import com.evstation.batteryswap.dto.response.BatteryResponse;

import java.util.List;

public interface BatteryService {
    List<BatteryResponse> getAll();
    BatteryResponse getById(Long id);
    BatteryResponse create(BatteryRequest request);
    BatteryResponse update(Long id, BatteryRequest request);
    void delete(Long id);
}
