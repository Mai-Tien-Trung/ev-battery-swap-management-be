package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    // Staff-Station queries
    List<User> findByRoleAndAssignedStationId(Role role, Long stationId);

    Optional<User> findByIdAndRole(Long id, Role role);
}