package com.evstation.batteryswap.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho API lấy danh sách pin từ reservation
 * Dùng để staff confirm swap khi user có đặt lịch
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationBatteriesResponse {
    private Long reservationId;
    private String username;
    private String vehicleVin;
    private String stationName;
    private String status;
    private Integer quantity;
    private Integer usedCount; // Số pin đã swap
    private LocalDateTime reservedAt;
    private LocalDateTime expireAt;
    private Long remainingMinutes; // Thời gian còn lại
    private List<ReservedBatteryInfo> batteries; // Danh sách pin đã đặt

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReservedBatteryInfo {
        private Long batterySerialId;
        private String serialNumber;
        private String batteryModel;
        private Double chargePercent;
        private Double stateOfHealth;
        private Double totalCycleCount;
        private String status; // RESERVED, IN_USE, etc.
    }
}
