package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.PlanTierRate;
import com.evstation.batteryswap.enums.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanTierRateRepository extends JpaRepository<PlanTierRate, Long> {
    List<PlanTierRate> findByPlanTypeOrderByMinValueAsc(PlanType planType);
}
