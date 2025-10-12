package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.SubscriptionPlanRequest;
import com.evstation.batteryswap.dto.response.SubscriptionPlanResponse;
import com.evstation.batteryswap.entity.SubscriptionPlan;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.repository.SubscriptionPlanRepository;
import com.evstation.batteryswap.service.SubscriptionPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    private SubscriptionPlanResponse mapToResponse(SubscriptionPlan plan) {
        SubscriptionPlanResponse res = new SubscriptionPlanResponse();
        res.setId(plan.getId());
        res.setName(plan.getName());
        res.setPrice(plan.getPrice());
        res.setDurationDays(plan.getDurationDays());
        res.setMaxBatteries(plan.getMaxBatteries());
        res.setBaseMileage(plan.getBaseMileage());
        res.setBaseEnergy(plan.getBaseEnergy());
        res.setPlanType(plan.getPlanType());
        res.setStatus(plan.getStatus());
        return res;
    }

    private SubscriptionPlan mapToEntity(SubscriptionPlanRequest request) {
        return SubscriptionPlan.builder()
                .name(request.getName())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .maxBatteries(request.getMaxBatteries())
                .baseMileage(request.getBaseMileage())
                .baseEnergy(request.getBaseEnergy())
                .planType(request.getPlanType())
                .status(request.getStatus())
                .build();
    }

    @Override
    public SubscriptionPlanResponse create(SubscriptionPlanRequest request) {
        validatePlan(request);
        SubscriptionPlan plan = mapToEntity(request);
        return mapToResponse(subscriptionPlanRepository.save(plan));
    }

    @Override
    public List<SubscriptionPlanResponse> getAll() {
        return subscriptionPlanRepository.findAll()
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionPlanResponse getById(Long id) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));
        return mapToResponse(plan);
    }

    @Override
    public SubscriptionPlanResponse update(Long id, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        plan.setName(request.getName());
        plan.setPrice(request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        plan.setMaxBatteries(request.getMaxBatteries());
        plan.setBaseMileage(request.getBaseMileage());
        plan.setBaseEnergy(request.getBaseEnergy());
        plan.setPlanType(request.getPlanType());
        plan.setStatus(request.getStatus());

        return mapToResponse(subscriptionPlanRepository.save(plan));
    }

    @Override
    public void delete(Long id) {
        subscriptionPlanRepository.deleteById(id);
    }

    private void validatePlan(SubscriptionPlanRequest request) {
        PlanType type = request.getPlanType();
        if (type == null) {
            throw new IllegalArgumentException("Plan type cannot be null (must be DISTANCE, ENERGY, or UNLIMITED)");
        }

        if (type == PlanType.DISTANCE) {
            Double mileage = request.getBaseMileage();
            if (mileage == null) {
                throw new IllegalArgumentException("Distance plan must have valid baseMileage (km)");
            }
            if (mileage <= 0) {
                throw new IllegalArgumentException("Distance plan must have positive baseMileage (km)");
            }
            request.setBaseEnergy(null);
        }
        else if (type == PlanType.ENERGY) {
            Double energy = request.getBaseEnergy();
            if (energy == null) {
                throw new IllegalArgumentException("Energy plan must have valid baseEnergy (kWh)");
            }
            if (energy <= 0) {
                throw new IllegalArgumentException("Energy plan must have positive baseEnergy (kWh)");
            }
            request.setBaseMileage(null);
        }
        else if (type == PlanType.UNLIMITED) {
            request.setBaseMileage(null);
            request.setBaseEnergy(null);
        }
    }


}
