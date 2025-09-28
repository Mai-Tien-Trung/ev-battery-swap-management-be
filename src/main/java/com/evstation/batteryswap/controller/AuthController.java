package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.LoginRequest;
import com.evstation.batteryswap.dto.request.RegisterRequest;
import com.evstation.batteryswap.dto.response.AuthResponse;
import com.evstation.batteryswap.dto.response.UserInfoResponse;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ✅ Đăng ký
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // ✅ Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ✅ Lấy thông tin user hiện tại
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(authService.getUserInfo(userId));
    }

    // ✅ Cập nhật thông tin user (phone, address)
    @PutMapping("/me")
    public ResponseEntity<UserInfoResponse> updateUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, String> updates) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(authService.updateUserInfo(userId, updates));
    }
}
