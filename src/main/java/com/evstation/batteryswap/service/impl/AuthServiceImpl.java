package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.LoginRequest;
import com.evstation.batteryswap.dto.request.RegisterRequest;
import com.evstation.batteryswap.dto.response.AuthResponse;
import com.evstation.batteryswap.dto.response.UserInfoResponse;

import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.enums.Role;
import com.evstation.batteryswap.repository.UserRepository;
import com.evstation.batteryswap.security.JwtService;
import com.evstation.batteryswap.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username đã tồn tại");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setPhone(null);
        user.setAddress(null);

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                "Register thành công",
                token,
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                "Login thành công",
                token,
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserInfoResponse(
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress()
        );
    }

    @Override
    public UserInfoResponse updateUserInfo(Long userId, Map<String, String> updates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.containsKey("phone")) {
            user.setPhone(updates.get("phone"));
        }
        if (updates.containsKey("address")) {
            user.setAddress(updates.get("address"));
        }

        userRepository.save(user);

        return new UserInfoResponse(
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress()
        );
    }
}
