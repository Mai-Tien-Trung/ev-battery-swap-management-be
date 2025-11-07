package com.evstation.batteryswap.dto.response;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VehicleWithBatteriesResponse {
    private Long vehicleId;
    private String vin;
    private String modelName;
    private String planName;
    private String subscriptionStatus;
    private List<BatterySummaryResponse> batteries;
}
