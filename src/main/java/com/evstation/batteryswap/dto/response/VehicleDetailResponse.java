package com.evstation.batteryswap.dto.response;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VehicleDetailResponse {
    private Long vehicleId;
    private String vin;
    private String modelName;
    private SubscriptionResponse subscription;
    private List<BatterySummaryResponse> batteries;
}
