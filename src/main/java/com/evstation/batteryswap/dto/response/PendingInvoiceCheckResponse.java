package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingInvoiceCheckResponse {
    
    private Long subscriptionId;
    private Boolean hasPendingInvoices;
    private Integer pendingCount;
    private Double totalPendingAmount;
}
