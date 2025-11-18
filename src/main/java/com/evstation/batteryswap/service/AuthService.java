package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.LoginRequest;
import com.evstation.batteryswap.dto.request.RegisterRequest;
import com.evstation.batteryswap.dto.request.UpdateProfileRequest;
import com.evstation.batteryswap.dto.response.AuthResponse;
import com.evstation.batteryswap.dto.response.UserInfoResponse;

import java.util.Map;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserInfoResponse getUserInfo(Long userId);
    UserInfoResponse updateUserInfo(Long userId, UpdateProfileRequest request);
}
