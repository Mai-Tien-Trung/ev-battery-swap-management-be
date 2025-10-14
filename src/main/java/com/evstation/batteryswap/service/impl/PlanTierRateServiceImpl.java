package com.evstation.batteryswap.service.impl;


import com.evstation.batteryswap.dto.request.PlanTierRateRequest;
import com.evstation.batteryswap.dto.response.PlanTierRateResponse;
import com.evstation.batteryswap.entity.PlanTierRate;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.repository.PlanTierRateRepository;
import com.evstation.batteryswap.service.PlanTierRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlanTierRateServiceImpl implements PlanTierRateService {

    private final PlanTierRateRepository planTierRateRepository;

    @Override
    public List<PlanTierRateResponse> getAll() {
        return planTierRateRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlanTierRateResponse> getByPlanType(PlanType planType) {
        return planTierRateRepository.findByPlanTypeOrderByMinValueAsc(planType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PlanTierRateResponse create(PlanTierRateRequest req) {
        PlanTierRate entity = PlanTierRate.builder()
                .planType(req.getPlanType())
                .minValue(req.getMinValue())
                .maxValue(req.getMaxValue())
                .rate(req.getRate())
                .note(req.getNote())
                .build();

        return toResponse(planTierRateRepository.save(entity));
    }

    @Override
    public PlanTierRateResponse update(Long id, PlanTierRateRequest req) {
        PlanTierRate tier = planTierRateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tier not found"));

        tier.setPlanType(req.getPlanType());
        tier.setMinValue(req.getMinValue());
        tier.setMaxValue(req.getMaxValue());
        tier.setRate(req.getRate());
        tier.setNote(req.getNote());

        return toResponse(planTierRateRepository.save(tier));
    }

    @Override
    public void delete(Long id) {
        planTierRateRepository.deleteById(id);
    }

    // Convert entity -> response
    private PlanTierRateResponse toResponse(PlanTierRate e) {
        return PlanTierRateResponse.builder()
                .id(e.getId())
                .planType(e.getPlanType())
                .minValue(e.getMinValue())
                .maxValue(e.getMaxValue())
                .rate(e.getRate())
                .build();
    }
}
