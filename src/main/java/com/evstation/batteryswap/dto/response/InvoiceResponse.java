package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.InvoiceStatus;
import com.evstation.batteryswap.enums.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {
    
    private Long invoiceId;
    private Long subscriptionId;
    private Long swapTransactionId;
    
    // Thông tin xe
    private String vehicleVin;
    private String vehicleModel;
    
    // Thông tin gói
    private String planName;
    private PlanType planType; // DISTANCE hoặc ENERGY
    
    // Thông tin hóa đơn
    private Double amount;
    private InvoiceStatus status;
    private String description; // "Overage: 1.5 kWh × 13,826₫/kWh = 20,739₫"
    
    // Chi tiết overage
    private Double overage; // Số lượng vượt
    private Double rate;    // Đơn giá tier
    private String unit;    // "km" hoặc "kWh"
    
    // Thời gian
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
