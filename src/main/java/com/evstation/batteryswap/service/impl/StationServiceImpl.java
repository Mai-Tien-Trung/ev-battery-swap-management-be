package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.StationRequest;
import com.evstation.batteryswap.dto.response.StationResponse;
import com.evstation.batteryswap.dto.response.StationSummaryResponse;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.StationStatus;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.StationRepository;
import com.evstation.batteryswap.service.StationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final BatterySerialRepository batterySerialRepository; // ✅ thêm dòng này

    public StationServiceImpl(StationRepository stationRepository,
                              BatterySerialRepository batterySerialRepository) {
        this.stationRepository = stationRepository;
        this.batterySerialRepository = batterySerialRepository;
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

    // ✅ Hàm tính toán số lượng pin thực tế trong trạm
    public void updateStationUsage(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new RuntimeException("Station not found"));

        long total = batterySerialRepository.countByStationId(stationId);
        long usable = batterySerialRepository.countByStationIdAndStatusNot(stationId, BatteryStatus.MAINTENANCE);
        long maintenance = total - usable;

        System.out.println(" Station " + station.getName() + ": "
                + usable + "/" + station.getCapacity() + " usable, "
                + maintenance + " under maintenance.");

        // cập nhật trạng thái tổng thể trạm
        if (usable >= station.getCapacity()) {
            station.setStatus(StationStatus.FULL);
        } else if (usable == 0) {
            station.setStatus(StationStatus.EMPTY);
        } else {
            station.setStatus(StationStatus.ACTIVE);
        }

        stationRepository.save(station);
    }
    @Override
    public StationSummaryResponse getStationSummary(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found"));

        return buildSummary(station);
    }

    @Override
    public List<StationSummaryResponse> getAllStationSummaries() {
        return stationRepository.findAll().stream()
                .map(this::buildSummary)
                .collect(Collectors.toList());
    }

    private StationSummaryResponse buildSummary(Station station) {
        long total = batterySerialRepository.countByStationId(station.getId());
        long maintenance = batterySerialRepository.countByStationIdAndStatus(station.getId(), BatteryStatus.MAINTENANCE);
        long usable = total - maintenance;

        return StationSummaryResponse.builder()
                .stationId(station.getId())
                .stationName(station.getName())
                .capacity(station.getCapacity())
                .totalBatteries(total)
                .usableBatteries(usable)
                .maintenanceBatteries(maintenance)
                .status(station.getStatus())
                .build();
    }
}
