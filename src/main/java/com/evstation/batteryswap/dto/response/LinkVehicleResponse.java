package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LinkVehicleResponse {
    private String message;
    private VehicleSummaryResponse vehicle;
    private SubscriptionResponse subscription;
    private List<BatterySummaryResponse> batteries;

}
