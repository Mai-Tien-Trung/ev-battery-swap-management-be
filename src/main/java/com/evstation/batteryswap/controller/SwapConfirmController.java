package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.SwapConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/swap")
@RequiredArgsConstructor
public class SwapConfirmController {

    private final SwapConfirmService swapConfirmService;

    // ✅ Nhân viên xác nhận swap
    @PutMapping("/{transactionId}/confirm")
    public ResponseEntity<String> confirmSwap(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal CustomUserDetails staff
    ) {
        String result = swapConfirmService.confirmSwap(transactionId, staff.getId());
        return ResponseEntity.ok(result);
    }

    // ❌ Nhân viên từ chối swap
    @PutMapping("/{transactionId}/reject")
    public ResponseEntity<String> rejectSwap(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal CustomUserDetails staff
    ) {
        String result = swapConfirmService.rejectSwap(transactionId, staff.getId());
        return ResponseEntity.ok(result);
    }
}
