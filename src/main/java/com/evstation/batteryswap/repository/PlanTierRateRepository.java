package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.PlanTierRate;
import com.evstation.batteryswap.enums.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanTierRateRepository extends JpaRepository<PlanTierRate, Long> {
    List<PlanTierRate> findByPlanTypeOrderByMinValueAsc(PlanType planType);
    @Query("""
           SELECT t FROM PlanTierRate t
           WHERE t.planType = :planType
             AND :value BETWEEN t.minValue AND COALESCE(t.maxValue, 999999)
           """)
    Optional<PlanTierRate> findTierRate(@Param("planType") PlanType planType,
                                        @Param("value") double value);
}
