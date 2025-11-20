package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.RegisterRequest;
import com.evstation.batteryswap.dto.request.UpdateUserRequest;
import com.evstation.batteryswap.dto.response.UserManagementResponse;
import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.enums.Role;
import com.evstation.batteryswap.repository.RefreshTokenRepository;
import com.evstation.batteryswap.repository.UserRepository;
import com.evstation.batteryswap.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final RefreshTokenRepository refreshTokenRepository;

        @Override
        public List<UserManagementResponse> getAllUsers() {
            return userRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        @Override
        public UserManagementResponse getUserById(Long id) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return mapToResponse(user);
        }

        @Override
        public UserManagementResponse createUser(RegisterRequest request) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new RuntimeException("Username already exists");
            }
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already exists");
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.USER); // Mặc định là USER, admin có thể update sau hoặc thêm field role vào request

            User savedUser = userRepository.save(user);
            return mapToResponse(savedUser);
        }

        @Override
        public UserManagementResponse updateUser(Long id, UpdateUserRequest request) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (request.getPhone() != null) user.setPhone(request.getPhone());
            if (request.getAddress() != null) user.setAddress(request.getAddress());
            if (request.getRole() != null) user.setRole(request.getRole());

            User updatedUser = userRepository.save(user);
            return mapToResponse(updatedUser);
        }

        @Override
        @Transactional
        public void deleteUser(Long id) {
            if (!userRepository.existsById(id)) {
                throw new RuntimeException("User not found");
            }
            // Xóa refresh token trước khi xóa user để tránh lỗi khóa ngoại
            refreshTokenRepository.deleteByUserId(id);
            userRepository.deleteById(id);
        }

        private UserManagementResponse mapToResponse(User user) {
            return UserManagementResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .address(user.getAddress())
                    .role(user.getRole())
                    .build();
        }
    }

