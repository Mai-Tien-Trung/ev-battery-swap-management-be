package com.evstation.batteryswap.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để hủy reservation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelReservationRequest {

    /**
     * Lý do hủy (optional)
     */
    private String reason;
}
