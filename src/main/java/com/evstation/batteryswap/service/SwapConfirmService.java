package com.evstation.batteryswap.service;

public interface SwapConfirmService {

    String confirmSwap(Long transactionId, Long staffId);

    String rejectSwap(Long transactionId, Long staffId);
}
