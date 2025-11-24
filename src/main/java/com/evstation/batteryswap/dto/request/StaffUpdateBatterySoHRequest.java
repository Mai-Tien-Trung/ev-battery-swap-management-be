package com.evstation.batteryswap.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffUpdateBatterySoHRequest {
    private Double newSoH; // New SoH value (must be 0-100)
}
