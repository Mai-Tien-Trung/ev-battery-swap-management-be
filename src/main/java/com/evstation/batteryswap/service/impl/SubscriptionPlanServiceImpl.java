package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.SubscriptionPlanRequest;
import com.evstation.batteryswap.dto.response.SubscriptionPlanResponse;
import com.evstation.batteryswap.entity.SubscriptionPlan;
import com.evstation.batteryswap.repository.SubscriptionPlanRepository;
import com.evstation.batteryswap.service.SubscriptionPlanService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository planRepository;

    public SubscriptionPlanServiceImpl(SubscriptionPlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    private SubscriptionPlanResponse mapToResponse(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .swapLimit(plan.getSwapLimit())
                .baseMileage(plan.getBaseMileage())
                .status(plan.getStatus())
                .build();
    }

    @Override
    public List<SubscriptionPlanResponse> getAll() {
        return planRepository.findAll()
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SubscriptionPlanResponse getById(Long id) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));
        return mapToResponse(plan);
    }

    @Override
    public SubscriptionPlanResponse create(SubscriptionPlanRequest request) {
        if (planRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên gói đã tồn tại");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .price(request.getPrice())
                .durationDays(request.getDurationDays())
                .swapLimit(request.getSwapLimit())
                .baseMileage(request.getBaseMileage())
                .status(request.getStatus())
                .build();

        return mapToResponse(planRepository.save(plan));
    }

    @Override
    public SubscriptionPlanResponse update(Long id, SubscriptionPlanRequest request) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        plan.setName(request.getName());
        plan.setPrice(request.getPrice());
        plan.setDurationDays(request.getDurationDays());
        plan.setSwapLimit(request.getSwapLimit());
        plan.setBaseMileage(request.getBaseMileage());
        plan.setStatus(request.getStatus());

        return mapToResponse(planRepository.save(plan));
    }

    @Override
    public void delete(Long id) {
        planRepository.deleteById(id);
    }
}
