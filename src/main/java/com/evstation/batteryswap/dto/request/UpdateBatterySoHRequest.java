package com.evstation.batteryswap.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBatterySoHRequest {
    private Double newSoH; // New SoH value (must be within plan's range)
}
