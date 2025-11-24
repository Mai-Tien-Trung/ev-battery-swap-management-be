package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.PlanStatus;
import com.evstation.batteryswap.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private int durationDays;

    @Column(nullable = false)
    private Integer maxBatteries;

    private Double baseMileage; // km cơ bản đi được
    private Double baseEnergy;

    // SoH range cho pin được phép đổi theo gói
    @Column(name = "min_soh")
    private Double minSoH; // SoH tối thiểu của pin được phép đổi

    @Column(name = "max_soh")
    private Double maxSoH; // SoH tối đa của pin được phép đổi

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.ACTIVE;
}
