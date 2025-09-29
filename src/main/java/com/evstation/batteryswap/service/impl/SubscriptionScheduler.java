package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    // chạy mỗi ngày lúc 0h00
    @Scheduled(cron = "0 * * * * ?")
    public void runAutoRenewJob() {
        subscriptionService.autoRenewSubscriptions();
    }
}
