package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    
    private Boolean success;
    private String message;
    
    // Thông tin invoice
    private Long invoiceId;
    private Double amount;
    private InvoiceStatus invoiceStatus;
    private String description;
    
    // Thông tin thanh toán
    private String transactionRef;
    private LocalDateTime paidAt;
    
    // VNPay info (optional)
    private String vnpTransactionNo;
    private String responseCode;
    private String bankCode;
}
