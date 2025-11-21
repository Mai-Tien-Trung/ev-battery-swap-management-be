package com.evstation.batteryswap.entity;


import com.evstation.batteryswap.enums.PlanType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plan_tier_rate")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanTierRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;  // DISTANCE / ENERGY

    @Column(nullable = false)
    private Double minValue;

    private Double maxValue;

    @Column(nullable = false)
    private Double rate;
    private String note;



}
