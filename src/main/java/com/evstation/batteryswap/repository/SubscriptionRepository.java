package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByVehicleIdAndStatus(Long vehicleId, SubscriptionStatus status);
}
