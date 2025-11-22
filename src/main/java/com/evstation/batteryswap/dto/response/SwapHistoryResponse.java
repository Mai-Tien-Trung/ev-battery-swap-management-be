package com.evstation.batteryswap.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwapHistoryResponse {
    private Long id; // Swap transaction ID
    private String stationName; // Tên trạm đã đổi
    private String oldBatterySerial; // Serial pin cũ
    private String newBatterySerial; // Serial pin mới
    private Double energyUsed; // Năng lượng đã dùng (kWh)
    private Double distance; // Quãng đường đã đi (km)
    private Double cost; // Chi phí (nếu có)
    private String status; // Trạng thái (COMPLETED, REJECTED)
    private LocalDateTime timestamp; // Thời gian đổi
    private LocalDateTime confirmedAt; // Thời gian staff xác nhận
}
