package com.evstation.batteryswap.service;


import com.evstation.batteryswap.dto.request.VehicleRequest;
import com.evstation.batteryswap.dto.response.VehicleResponse;

import java.util.List;

public interface VehicleService {
    VehicleResponse createVehicle(VehicleRequest request);
    List<VehicleResponse> getAllVehicles();
    VehicleResponse getVehicleById(Long id);
    VehicleResponse updateVehicle(Long id, VehicleRequest request);
    void deleteVehicle(Long id);
}

