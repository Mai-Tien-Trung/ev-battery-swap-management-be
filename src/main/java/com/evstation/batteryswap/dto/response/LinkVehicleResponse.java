package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LinkVehicleResponse {
    private String message;
    private VehicleResponse vehicle;
    private SubscriptionResponse subscription;
}
