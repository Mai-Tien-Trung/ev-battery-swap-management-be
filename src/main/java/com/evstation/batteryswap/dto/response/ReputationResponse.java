package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho thông tin uy tín reservation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReputationResponse {
    
    /**
     * Điểm uy tín hiện tại
     * Tối đa: 6 điểm/tháng
     * Tối thiểu: 0 điểm (không được đặt lịch)
     */
    private Integer currentReputation;
    
    /**
     * Điểm tối đa trong tháng
     */
    private Integer maxReputation;
    
    /**
     * Số lần hủy trong tháng này
     */
    private Integer cancelledCount;
    
    /**
     * Số lần hết hạn trong tháng này
     */
    private Integer expiredCount;
    
    /**
     * Số lần sử dụng thành công trong tháng này
     */
    private Integer usedCount;
    
    /**
     * Còn được phép đặt lịch không
     */
    private Boolean canReserve;
    
    /**
     * Thông báo
     */
    private String message;
}
