package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByUserIdAndVehicleIdAndStatus(Long userId, Long vehicleId, SubscriptionStatus status);
    Optional<Subscription> findByUserIdAndVehicleIdAndStatus(Long userId, Long vehicleId, SubscriptionStatus status);


}
