package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.LinkVehicleRequest;
import com.evstation.batteryswap.dto.response.LinkVehicleResponse;

public interface LinkVehicleService {
    LinkVehicleResponse linkVehicle(Long userId, LinkVehicleRequest request);
}
