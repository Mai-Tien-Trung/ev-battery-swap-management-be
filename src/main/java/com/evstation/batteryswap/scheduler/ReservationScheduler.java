package com.evstation.batteryswap.scheduler;

import com.evstation.batteryswap.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler để tự động expire reservations quá hạn
 * 
 * Đơn giản hóa: Chỉ gọi service method, business logic nằm trong service layer
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationService reservationService;

//    /**
//     * Chạy mỗi 1 phút để auto-expire reservations
//     * Cron: 0 */1 * * * ? (giây 0, mỗi 1 phút)
//     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void autoExpireReservations() {
        try {
            reservationService.autoExpireReservations();
        } catch (Exception e) {
            log.error("SCHEDULER ERROR: Failed to auto-expire reservations | error: {}",
                    e.getMessage(), e);
        }
    }
}

