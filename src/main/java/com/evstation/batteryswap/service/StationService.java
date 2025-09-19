package com.evstation.batteryswap.service;



import com.evstation.batteryswap.dto.request.StationRequest;
import com.evstation.batteryswap.dto.response.StationResponse;

import java.util.List;

public interface StationService {
    List<StationResponse> getAll();
    StationResponse getById(Long id);
    StationResponse create(StationRequest request);
    StationResponse update(Long id, StationRequest request);
    void delete(Long id);
}
