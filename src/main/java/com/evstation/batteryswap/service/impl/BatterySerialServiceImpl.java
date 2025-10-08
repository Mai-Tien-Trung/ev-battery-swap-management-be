package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.BatteryRequest;
import com.evstation.batteryswap.dto.response.BatteryResponse;
import com.evstation.batteryswap.entity.Battery;
import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.repository.BatteryRepository;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.StationRepository;
import com.evstation.batteryswap.service.BatterySerialService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BatterySerialServiceImpl implements BatterySerialService {

    private final BatterySerialRepository batterySerialRepository;
    private final BatteryRepository batteryRepository;
    private final StationRepository stationRepository;

    public BatterySerialServiceImpl(BatterySerialRepository batterySerialRepository,
                                    BatteryRepository batteryRepository,
                                    StationRepository stationRepository) {
        this.batterySerialRepository = batterySerialRepository;
        this.batteryRepository = batteryRepository;
        this.stationRepository = stationRepository;
    }

    private BatteryResponse mapToResponse(BatterySerial serial) {
        return BatteryResponse.builder()
                .id(serial.getId())
                .serialNumber(serial.getSerialNumber())
                .status(serial.getStatus())
                .swapCount(serial.getSwapCount())
                .stationId(serial.getStation() != null ? serial.getStation().getId() : null)
                .stationName(serial.getStation() != null ? serial.getStation().getName() : null)
                .build();
    }

    @Override
    public List<BatteryResponse> getAll() {
        return batterySerialRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BatteryResponse getById(Long id) {
        BatterySerial serial = batterySerialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Battery serial not found"));
        return mapToResponse(serial);
    }

    @Override
    public BatteryResponse create(BatteryRequest request) {
        // Tìm trạm
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        // Tìm loại pin (model)
        Battery batteryModel = batteryRepository.findById(request.getBatteryId())
                .orElseThrow(() -> new RuntimeException("Battery model not found"));

        // Gán dung lượng & SoH mặc định dựa theo Battery model
        Double capacity = batteryModel.getDesignCapacity();

        BatterySerial serial = BatterySerial.builder()
                .serialNumber(request.getSerialNumber())
                .status(request.getStatus())
                .station(station)
                .battery(batteryModel)
                .initialCapacity(capacity)
                .currentCapacity(capacity)
                .stateOfHealth(100.0)
                .build();

        batterySerialRepository.save(serial);

        return BatteryResponse.builder()
                .id(serial.getId())
                .serialNumber(serial.getSerialNumber())
                .status(serial.getStatus())
                .swapCount(serial.getSwapCount())
                .stationId(station.getId())
                .stationName(station.getName())
                .build();
    }


    @Override
    public BatteryResponse update(Long id, BatteryRequest request) {
        BatterySerial serial = batterySerialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Battery serial not found"));

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        serial.setSerialNumber(request.getSerialNumber());
        serial.setStatus(request.getStatus());
        serial.setStation(station);

        if (serial.getSwapCount() > 500 && serial.getStatus() == BatteryStatus.AVAILABLE) {
            serial.setStatus(BatteryStatus.MAINTENANCE);
        }

        return mapToResponse(batterySerialRepository.save(serial));
    }

    @Override
    public void delete(Long id) {
        if (!batterySerialRepository.existsById(id)) {
            throw new RuntimeException("Battery serial not found");
        }
        batterySerialRepository.deleteById(id);
    }
}
