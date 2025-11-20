package com.evstation.batteryswap.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO để tạo reservation
 * 
 * Ví dụ request:
 * {
 *   "vehicleId": 5,
 *   "stationId": 3,
 *   "quantity": 2,
 *   "batteryIds": [101, 102]  // Optional - chọn pin cụ thể
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    /**
     * ID của vehicle cần đổi pin
     * BẮT BUỘC - Phải thuộc về user đang request
     */
    @NotNull(message = "Vehicle ID is required")
    private Long vehicleId;

    /**
     * ID của trạm muốn đặt pin
     * BẮT BUỘC
     */
    @NotNull(message = "Station ID is required")
    private Long stationId;

    /**
     * Số lượng pin muốn đặt
     * BẮT BUỘC - Phải <= maxBatteries của plan
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    /**
     * Danh sách ID pin muốn đặt cụ thể (OPTIONAL)
     * 
     * - Nếu null/empty: Hệ thống tự động chọn pin tốt nhất (charge >= 95%)
     * - Nếu có giá trị: Validate pin phải AVAILABLE và thuộc trạm
     * 
     * Size của list phải = quantity
     */
    private List<Long> batteryIds;
}
