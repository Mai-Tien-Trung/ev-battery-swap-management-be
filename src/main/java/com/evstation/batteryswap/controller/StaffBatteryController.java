package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.response.BatteryHistoryResponse;
import com.evstation.batteryswap.dto.response.BatterySerialResponse;
import com.evstation.batteryswap.dto.response.StaffUpdateBatterySoHResponse;
import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.enums.BatteryEventType;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.UserRepository;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.BatteryHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/batteries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
@Slf4j
public class StaffBatteryController {

        private final BatterySerialRepository batterySerialRepository;
        private final UserRepository userRepository;
        private final BatteryHistoryService batteryHistoryService;

        /**
         * Get all batteries at staff's assigned station
         */
        @GetMapping
        public ResponseEntity<List<BatterySerialResponse>> getMyStationBatteries(
                        @AuthenticationPrincipal CustomUserDetails staffDetails) {

                // Get staff user
                User staff = userRepository.findById(staffDetails.getId())
                                .orElseThrow(() -> new RuntimeException("Staff not found"));

                // Check if staff has assigned station
                Station assignedStation = staff.getAssignedStation();
                if (assignedStation == null) {
                        throw new RuntimeException("Staff is not assigned to any station. Please contact admin.");
                }

                // Get batteries at assigned station
                List<BatterySerial> batteries = batterySerialRepository
                                .findByStationId(assignedStation.getId());

                log.info("STAFF VIEW BATTERIES | staffId={} | stationId={} | stationName={} | batteryCount={}",
                                staff.getId(), assignedStation.getId(), assignedStation.getName(), batteries.size());

                // Convert to response
                List<BatterySerialResponse> response = batteries.stream()
                                .map(this::toBatterySerialResponse)
                                .collect(Collectors.toList());

                return ResponseEntity.ok(response);
        }

        /**
         * Get specific battery details (must be at staff's station)
         */
        @GetMapping("/{batteryId}")
        public ResponseEntity<BatterySerialResponse> getBatteryDetails(
                        @PathVariable Long batteryId,
                        @AuthenticationPrincipal CustomUserDetails staffDetails) {

                // Get staff user
                User staff = userRepository.findById(staffDetails.getId())
                                .orElseThrow(() -> new RuntimeException("Staff not found"));

                Station assignedStation = staff.getAssignedStation();
                if (assignedStation == null) {
                        throw new RuntimeException("Staff is not assigned to any station");
                }

                // Get battery
                BatterySerial battery = batterySerialRepository.findById(batteryId)
                                .orElseThrow(() -> new RuntimeException("Battery not found"));

                // Verify battery is at staff's station
                if (battery.getStation() == null ||
                                !battery.getStation().getId().equals(assignedStation.getId())) {
                        throw new RuntimeException("Battery is not at your assigned station");
                }

                return ResponseEntity.ok(toBatterySerialResponse(battery));
        }

        /**
         * Get battery history (must be at staff's station)
         */
        @GetMapping("/{batteryId}/history")
        public ResponseEntity<com.evstation.batteryswap.dto.response.BatteryHistoryWithCountResponse> getBatteryHistory(
                        @PathVariable Long batteryId,
                        @AuthenticationPrincipal CustomUserDetails staffDetails) {

                // Get staff user
                User staff = userRepository.findById(staffDetails.getId())
                                .orElseThrow(() -> new RuntimeException("Staff not found"));

                Station assignedStation = staff.getAssignedStation();
                if (assignedStation == null) {
                        throw new RuntimeException("Staff is not assigned to any station");
                }

                // Get battery
                BatterySerial battery = batterySerialRepository.findById(batteryId)
                                .orElseThrow(() -> new RuntimeException("Battery not found"));

                // Verify battery is at staff's station
                if (battery.getStation() == null ||
                                !battery.getStation().getId().equals(assignedStation.getId())) {
                        throw new RuntimeException("Battery is not at your assigned station");
                }

                // Get history with count
                com.evstation.batteryswap.dto.response.BatteryHistoryWithCountResponse response = batteryHistoryService
                                .getBatteryHistoryWithCount(batteryId);

                return ResponseEntity.ok(response);
        }

        @PutMapping("/{batteryId}/soh")
        public ResponseEntity<StaffUpdateBatterySoHResponse> updateBatterySoH(
                        @PathVariable Long batteryId,
                        @RequestBody com.evstation.batteryswap.dto.request.StaffUpdateBatterySoHRequest request,
                        @AuthenticationPrincipal CustomUserDetails staffDetails) {

                // 1️⃣ Validate input
                if (request.getNewSoH() == null) {
                        throw new RuntimeException("New SoH value is required");
                }

                if (request.getNewSoH() < 0 || request.getNewSoH() > 100) {
                        throw new RuntimeException("SoH must be between 0 and 100");
                }

                // 2️⃣ Get staff user
                User staff = userRepository.findById(staffDetails.getId())
                                .orElseThrow(() -> new RuntimeException("Staff not found"));

                Station assignedStation = staff.getAssignedStation();
                if (assignedStation == null) {
                        throw new RuntimeException("Staff is not assigned to any station");
                }

                // 3️⃣ Get battery
                BatterySerial battery = batterySerialRepository.findById(batteryId)
                                .orElseThrow(() -> new RuntimeException("Battery not found"));

                // 4️⃣ Verify battery is at staff's station
                if (battery.getStation() == null ||
                                !battery.getStation().getId().equals(assignedStation.getId())) {
                        throw new RuntimeException("Battery is not at your assigned station");
                }

                // 5️⃣ Update SoH
                Double oldSoH = battery.getStateOfHealth() != null ? battery.getStateOfHealth() : 100.0;
                battery.setStateOfHealth(request.getNewSoH());
                batterySerialRepository.save(battery);

                // 6️⃣ Log to BatteryHistory
                batteryHistoryService.logEvent(
                                battery,
                                BatteryEventType.SOH_UPDATED,
                                String.format("SoH: %.1f%%", oldSoH),
                                String.format("SoH: %.1f%%", request.getNewSoH()),
                                assignedStation,
                                null,
                                staff,
                                String.format("Staff updated SoH from %.1f%% to %.1f%%", oldSoH, request.getNewSoH()),
                                request.getNewSoH()); // New SoH value after update

                log.info("STAFF UPDATE BATTERY SOH | staffId={} | batteryId={} | serialNumber={} | oldSoH={}% | newSoH={}% | station={}",
                                staff.getId(), batteryId, battery.getSerialNumber(), oldSoH, request.getNewSoH(),
                                assignedStation.getName());

                // 7️⃣ Build response
                return ResponseEntity.ok(
                                com.evstation.batteryswap.dto.response.StaffUpdateBatterySoHResponse.builder()
                                                .batteryId(batteryId)
                                                .serialNumber(battery.getSerialNumber())
                                                .oldSoH(oldSoH)
                                                .newSoH(request.getNewSoH())
                                                .stationId(assignedStation.getId())
                                                .stationName(assignedStation.getName())
                                                .message(String.format(
                                                                "Battery SoH updated successfully from %.1f%% to %.1f%%",
                                                                oldSoH, request.getNewSoH()))
                                                .build());
        }

        private BatterySerialResponse toBatterySerialResponse(BatterySerial battery) {
                return BatterySerialResponse.builder()
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
