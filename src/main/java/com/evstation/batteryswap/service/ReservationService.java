package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.ReservationRequest;
import com.evstation.batteryswap.dto.response.ReservationResponse;

import java.util.List;

/**
 * Service interface cho Reservation
 * 
 * Chức năng chính:
 * 1. Tạo reservation (đặt trước pin)
 * 2. Lấy danh sách reservations của user
 * 3. Hủy reservation
 * 4. Auto-expire reservations (cron job)
 */
public interface ReservationService {

    /**
     * Tạo reservation mới
     * 
     * Workflow:
     * 1. Validate user, vehicle, subscription
     * 2. Validate không có reservation ACTIVE cho vehicle này
     * 3. Validate quantity <= plan limit
     * 4. Tìm & lock batteries (AVAILABLE → RESERVED)
     * 5. Tạo Reservation entity (status = ACTIVE, expireAt = now + 1h)
     * 6. Tạo ReservationItems
     * 7. Return response với thông tin đầy đủ
     * 
     * @param userId ID của user (từ JWT token)
     * @param request ReservationRequest
     * @return ReservationResponse
     * @throws RuntimeException nếu validation failed
     */
    ReservationResponse createReservation(Long userId, ReservationRequest request);

    /**
     * Lấy reservation ACTIVE của user (nếu có)
     * 
     * @param userId ID của user
     * @return ReservationResponse hoặc null nếu không có
     */
    ReservationResponse getActiveReservation(Long userId, Long vehicleId);

    /**
     * Lấy tất cả reservations của user (mọi status)
     * Sắp xếp theo thời gian đặt mới nhất
     * 
     * @param userId ID của user
     * @return List<ReservationResponse>
     */
    List<ReservationResponse> getUserReservations(Long userId);

    /**
     * Lấy chi tiết reservation theo ID
     * 
     * @param userId ID của user (để validate ownership)
     * @param reservationId ID của reservation
     * @return ReservationResponse
     * @throws RuntimeException nếu không tìm thấy hoặc không thuộc user
     */
    ReservationResponse getReservationById(Long userId, Long reservationId);

    /**
     * Hủy reservation
     * 
     * Workflow:
     * 1. Validate reservation thuộc user
     * 2. Validate status = ACTIVE
     * 3. Release batteries (RESERVED → AVAILABLE)
     * 4. Update reservation status → CANCELLED
     * 5. Ghi lý do hủy
     * 
     * @param userId ID của user
     * @param reservationId ID của reservation
     * @param reason Lý do hủy (optional)
     * @return ReservationResponse
     * @throws RuntimeException nếu không thể hủy
     */
    ReservationResponse cancelReservation(Long userId, Long reservationId, String reason);

    /**
     * 🔄 CRON JOB: Auto-expire reservations quá hạn
     * 
     * Workflow:
     * 1. Tìm reservations: status = ACTIVE AND expireAt < now()
     * 2. Foreach reservation:
     *    - Release batteries (RESERVED → AVAILABLE)
     *    - Update status → EXPIRED
     * 3. Log kết quả
     * 
     * Chạy mỗi 1 phút
     */
    void autoExpireReservations();
}
