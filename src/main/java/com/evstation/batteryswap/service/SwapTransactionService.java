package com.evstation.batteryswap.service;


import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;

import java.util.List;
import java.util.Map;

public interface SwapTransactionService {
    SwapResponse processSwap(String username, SwapRequest request);
    List<Map<String, Object>> getMostFrequentSwapHour();
    List<Map<String, Object>> getSwapsPerStation();
}
