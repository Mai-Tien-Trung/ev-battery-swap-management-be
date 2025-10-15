package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;
import com.evstation.batteryswap.entity.SwapTransaction;
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


    @PostMapping
    public ResponseEntity<SwapResponse> swapBattery(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SwapRequest request
    ) {
        SwapResponse response = swapTransactionService.processSwap(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }



//     @GetMapping("/history")
//     public ResponseEntity<List<SwapTransaction>> getSwapHistory(
//             @AuthenticationPrincipal UserDetails userDetails
//     ) {
//         String username = userDetails.getUsername();
//         List<SwapTransaction> history = swapTransactionService.getUserSwapHistory(username);
//         return ResponseEntity.ok(history);
//     }
}
