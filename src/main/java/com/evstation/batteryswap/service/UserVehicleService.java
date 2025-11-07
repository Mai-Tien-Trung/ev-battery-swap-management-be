package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.response.VehicleWithBatteriesResponse;
import java.util.List;

public interface UserVehicleService {
    List<VehicleWithBatteriesResponse> getUserVehicles(Long userId);
}
