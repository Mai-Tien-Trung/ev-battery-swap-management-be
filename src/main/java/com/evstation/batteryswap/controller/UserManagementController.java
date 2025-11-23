package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.RegisterRequest;
import com.evstation.batteryswap.dto.request.UpdateUserRequest;
import com.evstation.batteryswap.dto.response.UserManagementResponse;
import com.evstation.batteryswap.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class UserManagementController {

    private final UserService userService;

    // 1. Lấy danh sách tất cả user
    @GetMapping
    public ResponseEntity<List<UserManagementResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // 2. Lấy chi tiết user theo ID
    @GetMapping("/{id}")
    public ResponseEntity<UserManagementResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // 3. Tạo user mới (Ví dụ tạo Staff)
    @PostMapping
    public ResponseEntity<UserManagementResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    // 4. Cập nhật user (Đổi quyền, thông tin)
    @PutMapping("/{id}")
    public ResponseEntity<UserManagementResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // 5. Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // 6. Get all STAFF members
    @GetMapping("/staff")
    public ResponseEntity<List<UserManagementResponse>> getAllStaff() {
        return ResponseEntity.ok(userService.getAllStaff());
    }

    // 7. Assign staff to station
    @PutMapping("/staff/{staffId}/assign-station")
    public ResponseEntity<UserManagementResponse> assignStaffToStation(
            @PathVariable Long staffId,
            @RequestBody com.evstation.batteryswap.dto.request.AssignStaffRequest request) {
        return ResponseEntity.ok(userService.assignStaffToStation(staffId, request));
    }
}
