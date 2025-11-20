package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO cho plan change request
 * Chứa thông tin subscription mới và invoice cần thanh toán
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanChangeResponse {
    
    // New subscription info
    private Long subscriptionId;
    private String status;
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double amount;
    
    // Invoice info
    private Long invoiceId;
    private Double invoiceAmount;
    
    // Messages
    private String message;
    private String note;
}
