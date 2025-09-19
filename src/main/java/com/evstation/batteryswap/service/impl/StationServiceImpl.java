package com.evstation.batteryswap.service.impl;


import com.evstation.batteryswap.dto.request.StationRequest;
import com.evstation.batteryswap.dto.response.StationResponse;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.enums.StationStatus;
import com.evstation.batteryswap.repository.StationRepository;
import com.evstation.batteryswap.service.StationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;

    // Constructor Injection (DI)
    public StationServiceImpl(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    private StationResponse mapToResponse(Station station) {
        return StationResponse.builder()
                .id(station.getId())
                .name(station.getName())
                .location(station.getLocation())
                .status(station.getStatus())
                .capacity(station.getCapacity())
                .phone(station.getPhone())
                .build();
    }

    @Override
    public List<StationResponse> getAll() {
        return stationRepository.findAll()
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StationResponse getById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found"));
        return mapToResponse(station);
    }

    @Override
    public StationResponse create(StationRequest request) {
        if (stationRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên trạm đã tồn tại");
        }

        Station station = Station.builder()
                .name(request.getName())
                .location(request.getLocation())
                .status(request.getStatus() != null ? request.getStatus() : StationStatus.ACTIVE)
                .capacity(request.getCapacity())
                .phone(request.getPhone())
                .build();

        return mapToResponse(stationRepository.save(station));
    }

    @Override
    public StationResponse update(Long id, StationRequest request) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found"));

        station.setName(request.getName());
        station.setLocation(request.getLocation());
        station.setStatus(request.getStatus());
        station.setCapacity(request.getCapacity());
        station.setPhone(request.getPhone());

        return mapToResponse(stationRepository.save(station));
    }

    @Override
    public void delete(Long id) {
        stationRepository.deleteById(id);
    }
}
