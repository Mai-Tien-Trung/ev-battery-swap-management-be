package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByJti(String jti);
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(Long userId);
}
