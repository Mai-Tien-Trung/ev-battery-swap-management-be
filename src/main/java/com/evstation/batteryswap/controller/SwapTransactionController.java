package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.repository.SwapTransactionRepository;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.SwapTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/swap")
@RequiredArgsConstructor
public class SwapTransactionController {

    private final SwapTransactionService swapTransactionService;
    private final SwapTransactionRepository swapTransactionRepository;


    @PostMapping
    public ResponseEntity<SwapResponse> processSwap(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SwapRequest request
    ) {
        String username = userDetails.getUsername();
        SwapResponse response = swapTransactionService.processSwap(username, request);
        return ResponseEntity.ok(response);
    }




}
