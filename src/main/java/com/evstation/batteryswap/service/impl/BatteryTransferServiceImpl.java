package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.BatteryTransferRequest;
import com.evstation.batteryswap.dto.response.BatteryTransferResponse;
import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.StationRepository;
import com.evstation.batteryswap.repository.UserRepository;
import com.evstation.batteryswap.service.BatteryTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatteryTransferServiceImpl implements BatteryTransferService {

        private final BatterySerialRepository batterySerialRepository;
        private final StationRepository stationRepository;
        private final UserRepository userRepository;
        private final com.evstation.batteryswap.service.BatteryHistoryService batteryHistoryService;

        @Override
        @Transactional
        public BatteryTransferResponse transferBattery(BatteryTransferRequest request, Long adminUserId) {
                // 1. Validate battery exists
                BatterySerial battery = batterySerialRepository.findById(request.getBatteryId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Battery not found with ID: " + request.getBatteryId()));

                // 2. Validate battery is AVAILABLE (not in use)
                if (battery.getStatus() != BatteryStatus.AVAILABLE) {
                        throw new RuntimeException(
                                        "Cannot transfer battery. Battery status must be AVAILABLE. Current status: "
                                                        + battery.getStatus());
                }

                // 3. Validate battery is currently at fromStation
                if (battery.getStation() == null) {
                        throw new RuntimeException("Battery is not assigned to any station");
                }

                if (!battery.getStation().getId().equals(request.getFromStationId())) {
                        throw new RuntimeException("Battery is not at the specified source station. " +
                                        "Current station: " + battery.getStation().getName() + " (ID: "
                                        + battery.getStation().getId() + ")");
                }

                // 4. Validate destination station exists
                Station toStation = stationRepository.findById(request.getToStationId())
                                .orElseThrow(() -> new RuntimeException(
                                                "Destination station not found with ID: " + request.getToStationId()));

                // 5. Validate not transferring to same station
                if (request.getFromStationId().equals(request.getToStationId())) {
                        throw new RuntimeException("Cannot transfer battery to the same station");
                }

                // 6. Get admin user info
                User admin = userRepository.findById(adminUserId)
                                .orElseThrow(() -> new RuntimeException("Admin user not found"));

                // 7. Store old station info for response
                Station fromStation = battery.getStation();
                String fromStationName = fromStation.getName();
                Long fromStationId = fromStation.getId();

                // 8. Perform transfer
                battery.setStation(toStation);
                battery.setUpdatedAt(LocalDateTime.now());
                BatterySerial updatedBattery = batterySerialRepository.save(battery);

                log.info("BATTERY TRANSFERRED | batteryId={} | serialNumber={} | from={}({}) | to={}({}) | by={}({})",
                                battery.getId(), battery.getSerialNumber(),
                                fromStationName, fromStationId,
                                toStation.getName(), toStation.getId(),
                                admin.getUsername(), admin.getId());

                // 📜 Log History
                batteryHistoryService.logEvent(
                                updatedBattery,
                                com.evstation.batteryswap.enums.BatteryEventType.TRANSFERRED,
                                "Station: " + fromStationName + " (ID: " + fromStationId + ")",
                                "Station: " + toStation.getName() + " (ID: " + toStation.getId() + ")",
                                toStation,
                                null,
                                admin,
                                "Transferred by admin: " + request.getNotes(),
                                updatedBattery.getStateOfHealth()); // Current SoH of battery

                // 9. Build response
                return BatteryTransferResponse.builder()
                                .batteryId(updatedBattery.getId())
                                .batterySerialNumber(updatedBattery.getSerialNumber())
                                .fromStationId(fromStationId)
                                .fromStationName(fromStationName)
                                .toStationId(toStation.getId())
                                .toStationName(toStation.getName())
                                .notes(request.getNotes())
                                .performedByUserId(admin.getId())
                                .performedByUsername(admin.getUsername())
                                .transferredAt(updatedBattery.getUpdatedAt())
                                .message("Battery transferred successfully from " + fromStationName + " to "
                                                + toStation.getName())
                                .build();
        }

        @Override
        public List<com.evstation.batteryswap.dto.response.BatterySerialResponse> getRecentlyUpdatedBatteries(
                        Long stationId, int limit) {
                List<BatterySerial> batteries;

                if (stationId != null) {
                        // Get batteries at specific station, sorted by recent updates
                        batteries = batterySerialRepository.findByStationId(stationId);
                } else {
                        // Get all batteries
                        batteries = batterySerialRepository.findAll();
                }

                // Sort by updatedAt descending and limit
                return batteries.stream()
                                .sorted((b1, b2) -> {
                                        LocalDateTime t1 = b1.getUpdatedAt() != null ? b1.getUpdatedAt()
                                                        : LocalDateTime.MIN;
                                        LocalDateTime t2 = b2.getUpdatedAt() != null ? b2.getUpdatedAt()
                                                        : LocalDateTime.MIN;
                                        return t2.compareTo(t1); // Descending order
                                })
                                .limit(limit)
                                .map(this::toBatterySerialResponse)
                                .collect(java.util.stream.Collectors.toList());
        }

        private com.evstation.batteryswap.dto.response.BatterySerialResponse toBatterySerialResponse(
                        BatterySerial battery) {
                return com.evstation.batteryswap.dto.response.BatterySerialResponse.builder()
                                .id(battery.getId())
                                .serialNumber(battery.getSerialNumber())
                                .status(battery.getStatus())
                                .stateOfHealth(battery.getStateOfHealth())
                                .chargePercent(battery.getChargePercent())
                                .currentCapacity(battery.getCurrentCapacity())
                                .initialCapacity(battery.getInitialCapacity())
                                .totalCycleCount(battery.getTotalCycleCount())
                                .swapCount(battery.getSwapCount())
                                .stationId(battery.getStation() != null ? battery.getStation().getId() : null)
                                .stationName(battery.getStation() != null ? battery.getStation().getName() : null)
                                .batteryModelId(battery.getBattery() != null ? battery.getBattery().getId() : null)
                                .batteryName(battery.getBattery() != null ? battery.getBattery().getName() : null)
                                .updatedAt(battery.getUpdatedAt())
                                .build();
        }
}
