package com.evstation.batteryswap.dto.response;


import com.evstation.batteryswap.enums.StationStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationResponse {
    private Long id;
    private String name;
    private String location;
    private StationStatus status;
    private int capacity;
    private String phone;
    private Double latitude;
    private Double longitude;
}
