package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.response.BatteryHistoryResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryEventType;
import com.evstation.batteryswap.repository.BatteryHistoryRepository;
import com.evstation.batteryswap.service.BatteryHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatteryHistoryServiceImpl implements BatteryHistoryService {

        private final BatteryHistoryRepository batteryHistoryRepository;

        @Override
        @Transactional
        public void logEvent(BatterySerial battery, BatteryEventType eventType,
                        String oldValue, String newValue,
                        Station station, Vehicle vehicle, User performedBy, String notes) {

                BatteryHistory history = BatteryHistory.builder()
                                .batterySerial(battery)
                                .eventType(eventType)
                                .oldValue(oldValue)
                                .newValue(newValue)
                                .station(station)
                                .vehicle(vehicle)
                                .performedBy(performedBy)
                                .notes(notes)
                                .createdAt(LocalDateTime.now())
                                .build();

                batteryHistoryRepository.save(history);

                log.info("BATTERY HISTORY LOGGED | batteryId={} | serialNumber={} | eventType={} | by={}",
                                battery.getId(), battery.getSerialNumber(), eventType,
                                performedBy != null ? performedBy.getUsername() : "SYSTEM");
        }

        @Override
        public List<BatteryHistoryResponse> getBatteryHistory(Long batterySerialId) {
                List<BatteryHistory> histories = batteryHistoryRepository
                                .findByBatterySerialIdOrderByCreatedAtDesc(batterySerialId);

                return histories.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public List<BatteryHistoryResponse> getBatteryHistoryByEventType(Long batterySerialId,
                        BatteryEventType eventType) {
                List<BatteryHistory> histories = batteryHistoryRepository
                                .findByBatterySerialIdAndEventTypeOrderByCreatedAtDesc(batterySerialId, eventType);

                return histories.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        private BatteryHistoryResponse toResponse(BatteryHistory history) {
                return BatteryHistoryResponse.builder()
                                .id(history.getId())
                                .eventType(history.getEventType())
                                .oldValue(history.getOldValue())
                                .newValue(history.getNewValue())
                                .stationId(history.getStation() != null ? history.getStation().getId() : null)
                                .stationName(history.getStation() != null ? history.getStation().getName() : null)
                                .vehicleId(history.getVehicle() != null ? history.getVehicle().getId() : null)
                                .vehicleVin(history.getVehicle() != null ? history.getVehicle().getVin() : null)
                                .performedByUserId(history.getPerformedBy() != null ? history.getPerformedBy().getId()
                                                : null)
                                .performedByUsername(history.getPerformedBy() != null
                                                ? history.getPerformedBy().getUsername()
                                                : null)
                                .notes(history.getNotes())
                                .createdAt(history.getCreatedAt())
                                .build();
        }
}
