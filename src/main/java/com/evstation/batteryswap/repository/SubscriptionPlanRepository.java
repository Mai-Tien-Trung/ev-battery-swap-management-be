package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    boolean existsByName(String name);
}
