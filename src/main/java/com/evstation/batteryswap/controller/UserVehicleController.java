package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.response.BatterySummaryResponse;
import com.evstation.batteryswap.dto.response.VehicleWithBatteriesResponse;
import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.entity.Vehicle;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.UserRepository;
import com.evstation.batteryswap.repository.VehicleRepository;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.UserVehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class UserVehicleController {

    private final UserVehicleService userVehicleService;
    private final VehicleRepository vehicleRepository;
    private final BatterySerialRepository batterySerialRepository;
    private final UserRepository userRepository;

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleWithBatteriesResponse>> getUserVehicles(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        List<VehicleWithBatteriesResponse> response = userVehicleService.getUserVehicles(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{vehicleId}/batteries")
    public ResponseEntity<List<BatterySummaryResponse>> getVehicleBatteries(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean ownsVehicle = user.getVehicles().stream()
                .anyMatch(v -> v.getId().equals(vehicleId));

        if (!ownsVehicle) {
            throw new RuntimeException("Vehicle does not belong to this user");
        }

        List<BatterySummaryResponse> batteries = batterySerialRepository.findByVehicleId(vehicleId)
                .stream()
                .map(b -> BatterySummaryResponse.builder()
                        .id(b.getId())
                        .serialNumber(b.getSerialNumber())
                        .status(b.getStatus().name())
                        .build())
                .toList();

        return ResponseEntity.ok(batteries);
    }

}
