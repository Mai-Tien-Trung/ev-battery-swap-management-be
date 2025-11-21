package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.RegisterRequest;
import com.evstation.batteryswap.dto.request.UpdateUserRequest;
import com.evstation.batteryswap.dto.response.UserManagementResponse;

import java.util.List;

public interface UserService {
    List<UserManagementResponse> getAllUsers();
    UserManagementResponse getUserById(Long id);
    UserManagementResponse createUser(RegisterRequest request); // Admin tạo user/staff mới
    UserManagementResponse updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
}
