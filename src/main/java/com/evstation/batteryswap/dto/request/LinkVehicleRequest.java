package com.evstation.batteryswap.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkVehicleRequest {
    private Long vehicleModelId;       // loại xe chọn từ dealer
    private Long subscriptionPlanId;   // gói đăng ký pin
}
