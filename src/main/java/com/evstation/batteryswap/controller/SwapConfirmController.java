package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.response.PendingSwapResponse;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import com.evstation.batteryswap.repository.SwapTransactionRepository;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.SwapConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/swap")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
public class SwapConfirmController {

    private final SwapConfirmService swapConfirmService;
    private final SwapTransactionRepository swapTransactionRepository;
    private final com.evstation.batteryswap.repository.SubscriptionRepository subscriptionRepository;
    private final com.evstation.batteryswap.repository.BatterySerialRepository batterySerialRepository;

    // ✅ Nhân viên xác nhận swap
    @PutMapping("/{transactionId}/confirm")
    public ResponseEntity<String> confirmSwap(
            @PathVariable Long transactionId,
            @RequestBody com.evstation.batteryswap.dto.request.StaffConfirmSwapRequest request,
            @AuthenticationPrincipal CustomUserDetails staff) {
        String result = swapConfirmService.confirmSwap(transactionId, staff.getId(), request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{transactionId}/reject")
    public ResponseEntity<String> rejectSwap(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal CustomUserDetails staff) {
        String result = swapConfirmService.rejectSwap(transactionId, staff.getId());
        return ResponseEntity.ok(result);
    }

    // 📋 Xem danh sách swap đang chờ xác nhận
    @GetMapping("/pending")
    public ResponseEntity<List<com.evstation.batteryswap.dto.response.PendingSwapResponse>> getPendingSwaps() {
        List<com.evstation.batteryswap.dto.response.PendingSwapResponse> pending = swapTransactionRepository
                .findByStatus(SwapTransactionStatus.PENDING_CONFIRM)
                .stream()
                .map(tx -> {
                    // Lấy subscription để biết SoH range
                    com.evstation.batteryswap.entity.Subscription sub = subscriptionRepository
                            .findByUserIdAndVehicleIdAndStatus(
                                    tx.getUser().getId(),
                                    tx.getVehicle().getId(),
                                    com.evstation.batteryswap.enums.SubscriptionStatus.ACTIVE)
                            .orElse(null);

                    // Lấy available batteries tại station, filter theo SoH range
                    List<com.evstation.batteryswap.dto.response.AvailableBatteryInfo> availableBatteries = batterySerialRepository
                            .findByStationAndStatus(
                                    tx.getStation(),
                                    com.evstation.batteryswap.enums.BatteryStatus.AVAILABLE)
                            .stream()
                            .filter(b -> b.getChargePercent() != null && b.getChargePercent() >= 95.0)
                            .filter(b -> {
                                // Filter theo SoH range nếu có subscription
                                if (sub != null && sub.getPlan().getMinSoH() != null
                                        && sub.getPlan().getMaxSoH() != null) {
                                    Double soh = java.util.Optional.ofNullable(b.getStateOfHealth()).orElse(100.0);
                                    return soh >= sub.getPlan().getMinSoH() && soh <= sub.getPlan().getMaxSoH();
                                }
                                return true; // Nếu không có SoH range thì show all
                            })
                            .map(b -> com.evstation.batteryswap.dto.response.AvailableBatteryInfo.builder()
                                    .id(b.getId())
                                    .serialNumber(b.getSerialNumber())
                                    .chargePercent(b.getChargePercent())
                                    .stateOfHealth(b.getStateOfHealth())
                                    .totalCycleCount(b.getTotalCycleCount())
                                    .batteryModel(b.getBattery().getName()) // Changed from getModel() to getName()
                                    .build())
                            .collect(java.util.stream.Collectors.toList());

                    return com.evstation.batteryswap.dto.response.PendingSwapResponse.builder()
                            .id(tx.getId())
                            .username(tx.getUser().getUsername())
                            .vehicleId(tx.getVehicle().getId())
                            .stationName(tx.getStation().getName())
                            .batterySerialNumber(tx.getBatterySerial().getSerialNumber())
                            .oldBatterySerialNumber(tx.getBatterySerial().getSerialNumber())
                            .oldBatteryChargePercent(tx.getBatterySerial().getChargePercent())
                            .oldBatterySoH(tx.getBatterySerial().getStateOfHealth())
                            .availableBatteries(availableBatteries)
                            .status(tx.getStatus().name())
                            .timestamp(tx.getTimestamp().toString())
                            .build();
                })
                .toList();

        if (pending.isEmpty())
            return ResponseEntity.noContent().build();
        return ResponseEntity.ok(pending);
    }
}
