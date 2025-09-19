package com.evstation.batteryswap.entity;

import com.evstation.batteryswap.enums.PlanStatus;
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
    private String name; // ví dụ: Gói 2 Pin, Gói 3 Pin

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private int durationDays; // thời hạn gói (ngày)

    @Column(nullable = false)
    private int swapLimit; // số lần đổi pin tối đa

    @Column(nullable = false)
    private double baseMileage; // km cơ bản đi được

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanStatus status = PlanStatus.ACTIVE;
}
