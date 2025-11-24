package com.evstation.batteryswap.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatteryHistoryWithCountResponse {
    private Long batteryId;
    private String serialNumber;
    private Integer totalSwapCount; // Tổng số lần swap (chỉ tính SWAPPED, không tính TRANSFERRED)
    private List<BatteryHistoryResponse> history;
}
