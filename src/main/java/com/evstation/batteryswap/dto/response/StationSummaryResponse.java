package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.StationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationSummaryResponse {
    private Long stationId;
    private String stationName;
    private int capacity;
    private long totalBatteries;
    private long usableBatteries;
    private long maintenanceBatteries;
    private StationStatus status;
}
