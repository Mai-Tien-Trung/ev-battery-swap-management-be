package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.UpdateProfileRequest;
import com.evstation.batteryswap.dto.response.UserInfoResponse;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user") // Endpoint chung cho user
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;


    @GetMapping("/profile")
    public ResponseEntity<UserInfoResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(authService.getUserInfo(userDetails.getId()));
    }


    @PutMapping("/profile")
    public ResponseEntity<UserInfoResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) { // Thêm @Valid
        return ResponseEntity.ok(authService.updateUserInfo(userDetails.getId(), request));
    }
}