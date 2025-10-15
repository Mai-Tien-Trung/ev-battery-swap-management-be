package com.evstation.batteryswap.service;


import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;

public interface SwapTransactionService {
    SwapResponse processSwap(String username, SwapRequest request);
}
