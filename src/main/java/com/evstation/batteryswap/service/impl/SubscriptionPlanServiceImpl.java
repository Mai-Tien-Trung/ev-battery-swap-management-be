package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.SubscriptionPlanRequest;
import com.evstation.batteryswap.dto.response.SubscriptionPlanResponse;
import com.evstation.batteryswap.entity.SubscriptionPlan;
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
                .status(request.getStatus())
                .build();
    }

    @Override
    public SubscriptionPlanResponse create(SubscriptionPlanRequest request) {
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
        plan.setStatus(request.getStatus());

        return mapToResponse(subscriptionPlanRepository.save(plan));
    }

    @Override
    public void delete(Long id) {
        subscriptionPlanRepository.deleteById(id);
    }
}
