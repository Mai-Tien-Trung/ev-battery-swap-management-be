package com.evstation.batteryswap.dto.request;


import com.evstation.batteryswap.enums.Role;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String phone;
    private String address;
    private Role role; // Admin có thể cấp quyền hoặc hạ quyền
}
