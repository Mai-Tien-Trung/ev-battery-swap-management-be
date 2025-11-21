package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.ReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho ReservationItem entity
 */
@Repository
public interface ReservationItemRepository extends JpaRepository<ReservationItem, Long> {

    /**
     * Tìm tất cả items của reservation
     * 
     * @param reservationId ID của reservation
     * @return List<ReservationItem>
     */
    List<ReservationItem> findByReservationId(Long reservationId);

    /**
     * Xóa tất cả items của reservation
     * Dùng khi cancel reservation
     * 
     * @param reservationId ID của reservation
     */
    void deleteByReservationId(Long reservationId);
}
