package com.evstation.batteryswap.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffConfirmSwapRequest {
    private double endPercent; // % pin cuối cùng của pin cũ (staff đo và nhập)
    private Long newBatterySerialId; // ID của pin mới cụ thể để đổi
}
