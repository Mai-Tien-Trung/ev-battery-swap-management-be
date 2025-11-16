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
import com.evstation.batteryswap.service.StationService;
import com.evstation.batteryswap.utils.BatterySerialUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatterySerialServiceImpl implements BatterySerialService {

    private final BatterySerialRepository batterySerialRepository;
    private final BatteryRepository batteryRepository;
    private final StationRepository stationRepository;
    private final StationService stationService;



    private BatteryResponse mapToResponse(BatterySerial serial) {
        return BatteryResponse.builder()
                .id(serial.getId())
                .serialNumber(serial.getSerialNumber())
                .status(serial.getStatus())
                .stationId(serial.getStation() != null ? serial.getStation().getId() : null)
                .stationName(serial.getStation() != null ? serial.getStation().getName() : null)
                .build();
    }

    @Override
    public List<BatteryResponse> getAll() {
        return batterySerialRepository.findAll()
                .stream().map(this::mapToResponse)
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
        // 🔹 Lấy model pin (Battery)
        Battery batteryModel = batteryRepository.findById(request.getBatteryId())
                .orElseThrow(() -> new RuntimeException("Battery model not found"));

        // 🔹 Lấy trạm
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        // 🔹 Kiểm tra capacity trạm
        long currentCount = batterySerialRepository.countByStationId(station.getId());
        if (currentCount >= station.getCapacity()) {
            throw new RuntimeException("Station " + station.getName() + " đã đầy ("
                    + currentCount + "/" + station.getCapacity() + " pin). Không thể thêm pin mới.");
        }

        // 🔹 Sinh serialNumber ngẫu nhiên thống nhất
        String randomSerial = BatterySerialUtil.generateSerialNumber();

        // 🔹 Tạo mới BatterySerial
        BatterySerial serial = BatterySerial.builder()
                .serialNumber(randomSerial)
                .status(request.getStatus() != null ? request.getStatus() : BatteryStatus.AVAILABLE)
                .station(station)
                .battery(batteryModel)
                .initialCapacity(batteryModel.getDesignCapacity())
                .currentCapacity(batteryModel.getDesignCapacity())
                .stateOfHealth(100.0)
                .totalCycleCount(0.0)
                .chargePercent(100.0)
                .build();

        batterySerialRepository.save(serial);

        // 🔹 Cập nhật lại trạng thái trạm sau khi thêm pin
        stationService.updateStationUsage(station.getId());

        return mapToResponse(serial);
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

        batterySerialRepository.save(serial);

        // 🔹 Cập nhật lại trạm sau khi đổi trạng thái pin
        stationService.updateStationUsage(station.getId());

        return mapToResponse(serial);
    }

    @Override
    public void delete(Long id) {
        BatterySerial serial = batterySerialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Battery serial not found"));

        Station station = serial.getStation();
        batterySerialRepository.deleteById(id);

        // 🔹 Cập nhật trạm sau khi xóa pin
        if (station != null) {
            stationService.updateStationUsage(station.getId());
        }
    }
    @Override
    public BatterySerial updateStatus(Long id, BatteryStatus status) {
        BatterySerial serial = batterySerialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Battery serial not found"));
        serial.setStatus(status);
        serial.setUpdatedAt(LocalDateTime.now());
        batterySerialRepository.save(serial);

        // Cập nhật trạng thái trạm sau khi đổi
        stationService.updateStationUsage(serial.getStation().getId());

        return serial;
    }
    @Override
    public List<Map<String, Object>> getBatteryStatusDistribution() {
        List<Object[]> results = batterySerialRepository.findBatteryStatusDistribution();
        return results.stream().map(arr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("status", ((BatteryStatus) arr[0]).name()); // Trả về Tên (String)
            map.put("count", (Long) arr[1]);
            return map;
        }).collect(Collectors.toList());
    }
}
