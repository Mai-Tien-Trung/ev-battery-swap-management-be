package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho Reservation
 * 
 * Trả về thông tin đầy đủ về reservation đã tạo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private Long reservationId;
    private ReservationStatus status;
    
    /**
     * Thông tin vehicle
     */
    private VehicleInfo vehicle;
    
    /**
     * Thông tin station
     */
    private StationInfo station;
    
    /**
     * Số lượng pin đặt
     */
    private Integer quantity;
    
    /**
     * Danh sách pin đã đặt
     */
    private List<BatteryInfo> batteries;
    
    /**
     * Thời gian đặt
     */
    private LocalDateTime reservedAt;
    
    /**
     * Thời gian hết hạn (reserved_at + 1 hour)
     */
    private LocalDateTime expireAt;
    
    /**
     * Số phút còn lại (tính từ thời điểm trả response)
     */
    private Long remainingMinutes;
    
    /**
     * Message hướng dẫn user
     */
    private String message;
    
    /**
     * Thông tin swap transaction (nếu đã sử dụng)
     */
    private Long swapTransactionId;
    
    /**
     * Thời điểm sử dụng (nếu status = USED)
     */
    private LocalDateTime usedAt;
    
    /**
     * Lý do hủy (nếu status = CANCELLED)
     */
    private String cancelReason;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleInfo {
        private Long id;
        private String vin;
        private String modelName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StationInfo {
        private Long id;
        private String name;
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatteryInfo {
        private Long id;
        private String serialNumber;
        private Double chargePercent;
        private Double stateOfHealth;
    }
}
