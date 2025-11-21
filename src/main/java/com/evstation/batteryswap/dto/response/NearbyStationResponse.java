package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyStationResponse {
    private Long stationId;
    private String stationName;
    private String stationLocation;
    private Double latitude;
    private Double longitude;
    private Double distance; // km, rounded to 2 decimals
    private Integer availableBattery;
    private String phone;
    private String status;
}


