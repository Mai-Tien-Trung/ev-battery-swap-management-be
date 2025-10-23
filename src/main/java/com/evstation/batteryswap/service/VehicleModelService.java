package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.response.VehicleModelResponse;
import com.evstation.batteryswap.entity.VehicleModel;
import java.util.List;

public interface VehicleModelService {
    VehicleModel create(VehicleModel model);
    List<VehicleModelResponse> getAll();
    VehicleModelResponse getById(Long id);
    VehicleModel update(Long id, VehicleModel model);
    void delete(Long id);
}
