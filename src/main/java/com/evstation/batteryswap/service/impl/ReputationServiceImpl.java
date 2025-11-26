package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.response.ReputationResponse;
import com.evstation.batteryswap.entity.Reservation;
import com.evstation.batteryswap.enums.ReservationStatus;
import com.evstation.batteryswap.repository.ReservationRepository;
import com.evstation.batteryswap.service.ReputationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Implementation của ReputationService
 * 
 * Tính điểm uy tín dựa trên lịch sử reservations trong tháng
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReputationServiceImpl implements ReputationService {

    private final ReservationRepository reservationRepository;
    
    // Điểm tối đa trong tháng
    private static final int MAX_REPUTATION = 6;
    
    // Điểm trừ khi hủy
    private static final int CANCEL_PENALTY = 1;
    
    // Điểm trừ khi hết hạn
    private static final int EXPIRED_PENALTY = 2;

    @Override
    public ReputationResponse getUserReputation(Long userId) {
        log.info("GET USER REPUTATION | userId={}", userId);
        
        // Lấy tháng hiện tại
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        
        // Lấy tất cả reservations trong tháng
        List<Reservation> reservations = reservationRepository
                .findByUserIdAndReservedAtBetween(userId, startOfMonth, endOfMonth);
        
        // Đếm theo status
        long cancelledCount = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CANCELLED)
                .count();
        
        long expiredCount = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.EXPIRED)
                .count();
        
        long usedCount = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.USED)
                .count();
        
        // Tính điểm uy tín
        int reputation = MAX_REPUTATION 
                - (int)(cancelledCount * CANCEL_PENALTY)
                - (int)(expiredCount * EXPIRED_PENALTY);
        
        // Đảm bảo không âm
        reputation = Math.max(0, reputation);
        
        boolean canReserve = reputation > 0;
        
        String message;
        if (reputation > 3) {
            message = String.format("Uy tín tốt: %d/%d điểm. Bạn có thể đặt lịch bình thường.", 
                    reputation, MAX_REPUTATION);
        } else if (reputation > 0) {
            message = String.format("Uy tín thấp: %d/%d điểm. Hãy cẩn thận khi đặt lịch.", 
                    reputation, MAX_REPUTATION);
        } else {
            message = String.format("Hết uy tín (0/%d điểm). Bạn KHÔNG thể đặt lịch trong tháng này. " +
                    "Lý do: %d lần hủy (-%d điểm) + %d lần hết hạn (-%d điểm).", 
                    MAX_REPUTATION, 
                    cancelledCount, cancelledCount * CANCEL_PENALTY,
                    expiredCount, expiredCount * EXPIRED_PENALTY);
        }
        
        log.info("REPUTATION CALCULATED | userId={} | reputation={}/{} | cancelled={} | expired={} | used={} | canReserve={}",
                userId, reputation, MAX_REPUTATION, cancelledCount, expiredCount, usedCount, canReserve);
        
        return ReputationResponse.builder()
                .currentReputation(reputation)
                .maxReputation(MAX_REPUTATION)
                .cancelledCount((int)cancelledCount)
                .expiredCount((int)expiredCount)
                .usedCount((int)usedCount)
                .canReserve(canReserve)
                .message(message)
                .build();
    }

    @Override
    public boolean validateReputationForReservation(Long userId) {
        int reputation = calculateReputation(userId);
        
        if (reputation <= 0) {
            ReputationResponse reputationInfo = getUserReputation(userId);
            log.warn("REPUTATION CHECK FAILED | userId={} | reputation={} | cancelled={} | expired={}",
                    userId, reputation, reputationInfo.getCancelledCount(), reputationInfo.getExpiredCount());
            
            throw new RuntimeException(String.format(
                    "Bạn không thể đặt lịch do hết uy tín (0/%d điểm). " +
                    "Trong tháng này: %d lần hủy, %d lần hết hạn. " +
                    "Vui lòng đợi đến tháng sau để đặt lịch lại.",
                    MAX_REPUTATION,
                    reputationInfo.getCancelledCount(),
                    reputationInfo.getExpiredCount()
            ));
        }
        
        log.info("REPUTATION CHECK PASSED | userId={} | reputation={}/{}", 
                userId, reputation, MAX_REPUTATION);
        return true;
    }

    @Override
    public int calculateReputation(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        
        List<Reservation> reservations = reservationRepository
                .findByUserIdAndReservedAtBetween(userId, startOfMonth, endOfMonth);
        
        long cancelledCount = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CANCELLED)
                .count();
        
        long expiredCount = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.EXPIRED)
                .count();
        
        int reputation = MAX_REPUTATION 
                - (int)(cancelledCount * CANCEL_PENALTY)
                - (int)(expiredCount * EXPIRED_PENALTY);
        
        return Math.max(0, reputation);
    }
}
