package com.evstation.batteryswap.dto.request;


import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for VNPAY payment initialization request
 */
@Data
public class PaymentInitRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Long amount;

    private String bankCode;
    private String language;
    private String orderInfo;
}