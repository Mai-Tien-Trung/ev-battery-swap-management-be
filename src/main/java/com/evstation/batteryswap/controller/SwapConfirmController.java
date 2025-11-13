package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.response.PendingSwapResponse;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import com.evstation.batteryswap.repository.SwapTransactionRepository;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.SwapConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/swap")
@RequiredArgsConstructor
public class SwapConfirmController {

    private final SwapConfirmService swapConfirmService;
    private final SwapTransactionRepository swapTransactionRepository;

    // ✅ Nhân viên xác nhận swap
    @PutMapping("/{transactionId}/confirm")
    public ResponseEntity<String> confirmSwap(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal CustomUserDetails staff
    ) {
        String result = swapConfirmService.confirmSwap(transactionId, staff.getId());
        return ResponseEntity.ok(result);
    }


    @PutMapping("/{transactionId}/reject")
    public ResponseEntity<String> rejectSwap(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal CustomUserDetails staff
    ) {
        String result = swapConfirmService.rejectSwap(transactionId, staff.getId());
        return ResponseEntity.ok(result);
    }
    @GetMapping("/pending")
    public ResponseEntity<List<PendingSwapResponse>> getPendingSwaps() {
        List<PendingSwapResponse> pending = swapTransactionRepository
                .findByStatus(SwapTransactionStatus.PENDING_CONFIRM)
                .stream()
                .map(tx -> PendingSwapResponse.builder()
                        .id(tx.getId())
                        .username(tx.getUser().getUsername())
                        .vehicleId(tx.getVehicle().getId())
                        .stationName(tx.getStation().getName())
                        .batterySerialNumber(tx.getBatterySerial().getSerialNumber())
                        .status(tx.getStatus().name())
                        .timestamp(tx.getTimestamp().toString())
                        .build()
                )
                .toList();

        if (pending.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(pending);
    }
}
