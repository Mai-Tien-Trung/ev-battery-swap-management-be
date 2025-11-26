package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.response.ReputationResponse;

/**
 * Service quản lý uy tín đặt lịch của user
 * 
 * Quy tắc:
 * - Điểm tối đa: 6 điểm/tháng
 * - Hủy reservation: -1 điểm
 * - Hết hạn (không swap): -2 điểm
 * - Sử dụng thành công: không thưởng điểm (chỉ trừ khi vi phạm)
 * - Nếu điểm <= 0: KHÔNG được đặt lịch
 */
public interface ReputationService {
    
    /**
     * Lấy thông tin uy tín của user trong tháng hiện tại
     * 
     * @param userId ID của user
     * @return ReputationResponse
     */
    ReputationResponse getUserReputation(Long userId);
    
    /**
     * Kiểm tra user có đủ uy tín để đặt lịch không
     * 
     * @param userId ID của user
     * @return true nếu còn uy tín (> 0), false nếu hết uy tín (<= 0)
     * @throws RuntimeException nếu user hết uy tín
     */
    boolean validateReputationForReservation(Long userId);
    
    /**
     * Tính điểm uy tín hiện tại của user
     * 
     * @param userId ID của user
     * @return Điểm uy tín (0-6)
     */
    int calculateReputation(Long userId);
}
