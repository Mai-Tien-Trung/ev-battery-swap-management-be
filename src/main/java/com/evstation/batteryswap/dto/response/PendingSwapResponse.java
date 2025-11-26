package com.evstation.batteryswap.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingSwapResponse {
    private Long id;
    private String username;
    private Long vehicleId;
    private String vehicleVin; // Mã VIN của xe
    private String stationName;
    private String batterySerialNumber; // Pin cũ đang được trả về

    // Thông tin chi tiết pin cũ
    private String oldBatterySerialNumber;
    private Double oldBatteryChargePercent;
    private Double oldBatterySoH;

    // Danh sách pin available tại trạm (đã filter theo SoH range của plan)
    private List<AvailableBatteryInfo> availableBatteries;

    private String status;
    private String timestamp;

    // Thông tin reservation (nếu user đã đặt lịch)
    private ReservationInfo reservation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReservationInfo {
        private Long reservationId;
        private String status;
        private Integer quantity;
        private Integer usedCount;
        private LocalDateTime reservedAt;
        private LocalDateTime expireAt;
        private Long remainingMinutes;
        private List<ReservedBatteryDetail> batteries;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ReservedBatteryDetail {
            private Long batterySerialId;
            private String serialNumber;
            private String batteryModel;
            private Double chargePercent;
            private Double stateOfHealth;
            private Double totalCycleCount;
            private String status;
        }
    }
}
