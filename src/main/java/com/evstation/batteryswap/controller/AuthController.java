package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.RegisterRequest;
import com.evstation.batteryswap.entity.JwtProperties;
import com.evstation.batteryswap.entity.RefreshToken;
import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.repository.RefreshTokenRepository;
import com.evstation.batteryswap.repository.UserRepository;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.security.JwtService;
import com.evstation.batteryswap.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProps;
    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> req, HttpServletResponse resp) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.get("username"), req.get("password"))
        );

        User user = userRepo.findByUsername(req.get("username"))
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Save refresh token
        var claims = jwtService.parseClaims(refreshToken);
        RefreshToken ref = new RefreshToken();
        ref.setToken(refreshToken);
        ref.setJti(claims.getId());
        ref.setUserId(user.getId());
        ref.setExpiryDate(claims.getExpiration().toInstant());
        refreshRepo.save(ref);

        // Set HttpOnly cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true).secure(true)
                .sameSite("Strict")
                .path("/api/auth/refresh")
                .maxAge(jwtProps.getRefreshTtlMs() / 1000)
                .build();

        resp.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshCookie,
                                     @RequestBody(required = false) Map<String, String> body) {
        String refreshToken = refreshCookie != null ? refreshCookie : (body != null ? body.get("refreshToken") : null);
        if (refreshToken == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No refresh token provided");

        try {
            var claims = jwtService.parseClaims(refreshToken);
            String jti = claims.getId();
            RefreshToken stored = refreshRepo.findByJti(jti)
                    .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

            if (stored.getExpiryDate().isBefore(Instant.now())) {
                refreshRepo.delete(stored);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
            }

            User user = userRepo.findById(stored.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Rotate token
            refreshRepo.delete(stored);
            String newAccess = jwtService.generateAccessToken(user);
            String newRefresh = jwtService.generateRefreshToken(user);

            var newClaims = jwtService.parseClaims(newRefresh);
            RefreshToken newRef = new RefreshToken();
            newRef.setToken(newRefresh);
            newRef.setJti(newClaims.getId());
            newRef.setUserId(user.getId());
            newRef.setExpiryDate(newClaims.getExpiration().toInstant());
            refreshRepo.save(newRef);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefresh)
                    .httpOnly(true).secure(true).sameSite("Strict")
                    .path("/api/auth/refresh")
                    .maxAge(jwtProps.getRefreshTtlMs() / 1000)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("accessToken", newAccess));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole().name()
        ));
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                    @CookieValue(name = "refreshToken", required = false) String refreshCookie) {
        if (refreshCookie != null) {
            refreshRepo.findByToken(refreshCookie).ifPresent(refreshRepo::delete);
        }
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).maxAge(0).path("/api/auth/refresh").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }
}
