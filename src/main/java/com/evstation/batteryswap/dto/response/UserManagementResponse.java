package com.evstation.batteryswap.dto.response;

import com.evstation.batteryswap.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserManagementResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String address;
    private Role role;

    // For STAFF: assigned station info
    private Long assignedStationId;
    private String assignedStationName;
}
