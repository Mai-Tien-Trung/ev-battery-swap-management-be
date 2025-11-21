package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.entity.Invoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VNPayCallbackResult {
    
    private boolean success;
    private Invoice invoice; // Invoice object nếu thanh toán thành công
    private String transactionRef;
    private String vnpTransactionNo;
    private String responseCode;
    private String bankCode;
}
