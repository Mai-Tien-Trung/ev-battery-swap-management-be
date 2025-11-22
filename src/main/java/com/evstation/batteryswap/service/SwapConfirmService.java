package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.StaffConfirmSwapRequest;

public interface SwapConfirmService {

    String confirmSwap(Long transactionId, Long staffId, StaffConfirmSwapRequest request);

    String rejectSwap(Long transactionId, Long staffId);
}
