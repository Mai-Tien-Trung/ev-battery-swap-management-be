package com.evstation.batteryswap.scheduler;

import com.evstation.batteryswap.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler để tự động expire reservations quá hạn
 * 
 * Logic:
 * - Chạy mỗi 1 phút (cron: 0 */1 * * * ?)
 * - Tìm reservations có status = ACTIVE và expireAt < now()
 * - Release batteries: RESERVED → AVAILABLE
 * - Update reservation: ACTIVE → EXPIRED
 * 
 * Flow:
 * 1. User tạo reservation → expireAt = now + 1 giờ
 * 2. Scheduler check mỗi phút
 * 3. Nếu quá expireAt → Auto expire
 * 4. User có thể:
 *    - Swap trong thời gian → Reservation USED
 *    - Cancel → Reservation CANCELLED
 *    - Không làm gì → Scheduler expire → EXPIRED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationScheduler {

    private final ReservationService reservationService;

    /**
     * ========== CRON JOB: AUTO-EXPIRE RESERVATIONS ==========
     * 
     * Chạy mỗi 1 phút: 0 */1 * * * ?
     * - Giây: 0 (chạy vào giây thứ 0)
     * - Phút: */1 (mỗi 1 phút)
     * - Giờ: * (mọi giờ)
     * - Ngày: * (mọi ngày)
     * - Tháng: * (mọi tháng)
     * - Thứ: ? (không quan tâm thứ)
     * 
     * Ví dụ timeline:
     * - 10:00:00 → User tạo reservation (expireAt = 11:00:00)
     * - 10:01:00 → Scheduler chạy (chưa expire)
     * - 10:02:00 → Scheduler chạy (chưa expire)
     * - ...
     * - 11:00:00 → Scheduler chạy (chưa expire vì đúng expireAt)
     * - 11:01:00 → Scheduler chạy → EXPIRE (now > expireAt)
     * 
     * Alternative: Dùng fixedRate = 60000 (60 giây = 1 phút)
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void autoExpireReservations() {
        log.debug("SCHEDULER: Running auto-expire reservations job");

        try {
            reservationService.autoExpireReservations();
        } catch (Exception e) {
            log.error("SCHEDULER ERROR: Failed to auto-expire reservations | error: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * Alternative scheduler: Chạy mỗi 60 giây (60000 milliseconds)
     * 
     * Uncomment để dùng fixedRate thay vì cron:
     */
    // @Scheduled(fixedRate = 60000)
    // public void autoExpireReservationsFixedRate() {
    //     log.debug("SCHEDULER (FIXED-RATE): Running auto-expire reservations job");
    //     
    //     try {
    //         reservationService.autoExpireReservations();
    //     } catch (Exception e) {
    //         log.error("SCHEDULER ERROR: Failed to auto-expire reservations | error: {}",
    //                 e.getMessage(), e);
    //     }
    // }
}
