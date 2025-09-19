package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.BatteryRequest;
import com.evstation.batteryswap.dto.response.BatteryResponse;
import com.evstation.batteryswap.entity.Battery;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.repository.BatteryRepository;
import com.evstation.batteryswap.repository.StationRepository;
import com.evstation.batteryswap.service.BatteryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatteryServiceImpl implements BatteryService {

    private final BatteryRepository batteryRepository;
    private final StationRepository stationRepository;

    public BatteryServiceImpl(BatteryRepository batteryRepository, StationRepository stationRepository) {
        this.batteryRepository = batteryRepository;
        this.stationRepository = stationRepository;
    }

    private BatteryResponse mapToResponse(Battery battery) {
        return BatteryResponse.builder()
                .id(battery.getId())
                .serialNumber(battery.getSerialNumber())
                .status(battery.getStatus())
                .swapCount(battery.getSwapCount())
                .stationId(battery.getStation().getId())
                .stationName(battery.getStation().getName())
                .build();
    }

    @Override
    public List<BatteryResponse> getAll() {
        return batteryRepository.findAll()
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BatteryResponse getById(Long id) {
        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Battery not found"));
        return mapToResponse(battery);
    }

    @Override
    public BatteryResponse create(BatteryRequest request) {
        if (batteryRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new RuntimeException("Serial number đã tồn tại");
        }

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        Battery battery = Battery.builder()
                .serialNumber(request.getSerialNumber())
                .status(request.getStatus())
                .swapCount(request.getSwapCount())
                .station(station)
                .build();

        // rule: nếu swapCount quá cao -> MAINTENANCE
        if (battery.getSwapCount() > 500 && battery.getStatus() == BatteryStatus.AVAILABLE) {
            battery.setStatus(BatteryStatus.MAINTENANCE);
        }

        return mapToResponse(batteryRepository.save(battery));
    }

    @Override
    public BatteryResponse update(Long id, BatteryRequest request) {
        Battery battery = batteryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Battery not found"));

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        battery.setSerialNumber(request.getSerialNumber());
        battery.setStatus(request.getStatus());
        battery.setSwapCount(request.getSwapCount());
        battery.setStation(station);

        // rule: auto set MAINTENANCE nếu swapCount > 500
        if (battery.getSwapCount() > 500 && battery.getStatus() == BatteryStatus.AVAILABLE) {
            battery.setStatus(BatteryStatus.MAINTENANCE);
        }

        return mapToResponse(batteryRepository.save(battery));
    }

    @Override
    public void delete(Long id) {
        batteryRepository.deleteById(id);
    }
}
