package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.RegisterRequest;
import com.evstation.batteryswap.dto.request.UpdateUserRequest;
import com.evstation.batteryswap.dto.response.UserManagementResponse;

import java.util.List;

public interface UserService {
    List<UserManagementResponse> getAllUsers();

    List<UserManagementResponse> getAllStaff(); // Get all STAFF users

    UserManagementResponse getUserById(Long id);

    UserManagementResponse createUser(RegisterRequest request); // Admin tạo user/staff mới

    UserManagementResponse updateUser(Long id, UpdateUserRequest request);

    void deleteUser(Long id);

    UserManagementResponse assignStaffToStation(Long staffId,
            com.evstation.batteryswap.dto.request.AssignStaffRequest request);
}
