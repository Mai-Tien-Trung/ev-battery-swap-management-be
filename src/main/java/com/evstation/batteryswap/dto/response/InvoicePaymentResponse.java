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
public class InvoicePaymentResponse {
    
    private Boolean success;
    private String message;
    private Long invoiceId;
    private Double amount;
    private InvoiceStatus status;
    private LocalDateTime paidAt;
    private String warning; // For deprecated manual payment warning
}
