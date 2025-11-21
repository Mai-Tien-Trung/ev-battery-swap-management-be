package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Reservation;
import com.evstation.batteryswap.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho Reservation entity
 * 
 * Các query chính:
 * 1. Kiểm tra vehicle có reservation ACTIVE không (business rule)
 * 2. Tìm reservation của user/vehicle
 * 3. Tìm reservation quá hạn (cho cron job)
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Kiểm tra vehicle đã có reservation ACTIVE chưa
     * Business rule: Mỗi vehicle chỉ có 1 reservation ACTIVE tại 1 thời điểm
     * 
     * @param userId ID của user
     * @param vehicleId ID của vehicle
     * @param status Status cần check (thường là ACTIVE)
     * @return true nếu đã có reservation ACTIVE
     */
    boolean existsByUserIdAndVehicleIdAndStatus(
        Long userId, 
        Long vehicleId, 
        ReservationStatus status
    );

    /**
     * Tìm reservation ACTIVE của vehicle
     * 
     * @param userId ID của user
     * @param vehicleId ID của vehicle
     * @param status Status (ACTIVE)
     * @return Optional<Reservation>
     */
    Optional<Reservation> findByUserIdAndVehicleIdAndStatus(
        Long userId, 
        Long vehicleId, 
        ReservationStatus status
    );

    /**
     * Tìm reservation ACTIVE của vehicle tại trạm cụ thể
     * Dùng khi swap: check xem pin có thuộc reservation không
     * 
     * ⚠️ EAGER FETCH items để tránh LazyInitializationException
     * 
     * @param userId ID của user
     * @param vehicleId ID của vehicle
     * @param stationId ID của station
     * @param status Status (ACTIVE)
     * @return Optional<Reservation> with items loaded
     */
    @Query("SELECT r FROM Reservation r " +
           "LEFT JOIN FETCH r.items i " +
           "LEFT JOIN FETCH i.batterySerial " +
           "WHERE r.user.id = :userId " +
           "AND r.vehicle.id = :vehicleId " +
           "AND r.station.id = :stationId " +
           "AND r.status = :status")
    Optional<Reservation> findByUserIdAndVehicleIdAndStationIdAndStatus(
        @Param("userId") Long userId, 
        @Param("vehicleId") Long vehicleId, 
        @Param("stationId") Long stationId,
        @Param("status") ReservationStatus status
    );

    /**
     * Lấy tất cả reservations của user (mọi status)
     * Sắp xếp theo thời gian đặt mới nhất
     * 
     * @param userId ID của user
     * @return List<Reservation>
     */
    List<Reservation> findByUserIdOrderByReservedAtDesc(Long userId);

    /**
     * Lấy tất cả reservations của vehicle
     * 
     * @param vehicleId ID của vehicle
     * @return List<Reservation>
     */
    List<Reservation> findByVehicleIdOrderByReservedAtDesc(Long vehicleId);

    /**
     * Lấy reservations theo status của user
     * 
     * @param userId ID của user
     * @param status Status filter
     * @return List<Reservation>
     */
    List<Reservation> findByUserIdAndStatusOrderByReservedAtDesc(
        Long userId, 
        ReservationStatus status
    );

    /**
     * 🔄 CRON JOB: Tìm reservations đã hết hạn
     * Dùng cho auto-expire job chạy mỗi phút
     * 
     * Logic: status = ACTIVE AND expireAt < now()
     * 
     * @param status Status (ACTIVE)
     * @param expireAt Thời điểm hiện tại
     * @return List<Reservation> cần expire
     */
    List<Reservation> findByStatusAndExpireAtBefore(
        ReservationStatus status, 
        LocalDateTime expireAt
    );

    /**
     * Query custom: Tìm reservation với join fetch items
     * Tránh N+1 query problem
     * 
     * @param reservationId ID của reservation
     * @return Optional<Reservation> with items loaded
     */
    @Query("SELECT r FROM Reservation r " +
           "LEFT JOIN FETCH r.items i " +
           "LEFT JOIN FETCH i.batterySerial " +
           "WHERE r.id = :reservationId")
    Optional<Reservation> findByIdWithItems(@Param("reservationId") Long reservationId);

    /**
     * Đếm số reservations ACTIVE của user
     * 
     * @param userId ID của user
     * @param status Status (ACTIVE)
     * @return Số lượng reservations
     */
    long countByUserIdAndStatus(Long userId, ReservationStatus status);
}
