package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.repository.SwapTransactionRepository;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.SwapTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/swap")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class SwapTransactionController {

    private final SwapTransactionService swapTransactionService;
    private final SwapTransactionRepository swapTransactionRepository;

    @PostMapping
    public ResponseEntity<SwapResponse> createSwap(
            @RequestBody SwapRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SwapResponse response = swapTransactionService.processSwap(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    // 🆕 Endpoint để user xem lịch sử đổi pin
    @GetMapping("/history")
    public ResponseEntity<List<com.evstation.batteryswap.dto.response.SwapHistoryResponse>> getSwapHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<com.evstation.batteryswap.dto.response.SwapHistoryResponse> history = swapTransactionService
                .getUserSwapHistory(userDetails.getUsername());
        return ResponseEntity.ok(history);
    }
}
