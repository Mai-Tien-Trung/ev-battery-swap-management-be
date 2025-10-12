package com.evstation.batteryswap.dto.request;

import com.evstation.batteryswap.enums.PlanStatus;
import com.evstation.batteryswap.enums.PlanType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionPlanRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull(message = "Price cannot be null")
    @Min(value = 0, message = "Price must be >= 0")
    private Double price;

    @NotNull(message = "Duration cannot be null")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;

    @NotNull(message = "Max batteries cannot be null")
    @Min(value = 1, message = "Max batteries must be at least 1")
    private Integer maxBatteries;

    private Double baseMileage;
    private Double baseEnergy;
    private PlanType planType;


    private PlanStatus status = PlanStatus.ACTIVE;
}
