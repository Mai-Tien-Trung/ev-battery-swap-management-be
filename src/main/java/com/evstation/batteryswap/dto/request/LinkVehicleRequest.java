package com.evstation.batteryswap.dto.request;

import lombok.Data;

@Data
public class LinkVehicleRequest {
    private Long vehicleId;
    private Long subscriptionPlanId;
}
