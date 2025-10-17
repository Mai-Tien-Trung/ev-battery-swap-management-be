package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    @Column(name = "next_plan_id")
    private Long nextPlanId;
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(name = "energy_used_this_month")
    private Double energyUsedThisMonth = 0.0;

    @Column(name = "distance_used_this_month")
    private Double distanceUsedThisMonth = 0.0;
}