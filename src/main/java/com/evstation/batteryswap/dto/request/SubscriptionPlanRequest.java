package com.evstation.batteryswap.dto.request;

import com.evstation.batteryswap.enums.PlanStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanRequest {

    @NotBlank(message = "Tên gói không được để trống")
    private String name;

    @Positive(message = "Giá phải lớn hơn 0")
    private double price;

    @Min(value = 1, message = "Thời hạn tối thiểu là 1 ngày")
    private int durationDays;

    @Min(value = 1, message = "Số lần swap tối thiểu là 1")
    private int swapLimit;

    @Positive(message = "Quãng đường cơ bản phải lớn hơn 0")
    private double baseMileage;

    @NotNull(message = "Trạng thái không được để trống")
    private PlanStatus status;
}
