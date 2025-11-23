package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.UpdateBatterySoHRequest;
import com.evstation.batteryswap.dto.response.UpdateBatterySoHResponse;
import com.evstation.batteryswap.entity.BatterySerial;
import com.evstation.batteryswap.entity.Subscription;
import com.evstation.batteryswap.entity.SubscriptionPlan;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.BatterySerialRepository;
import com.evstation.batteryswap.repository.SubscriptionRepository;
import com.evstation.batteryswap.service.AdminBatteryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminBatteryServiceImpl implements AdminBatteryService {

    private final BatterySerialRepository batterySerialRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public UpdateBatterySoHResponse updateBatterySoH(Long batteryId, UpdateBatterySoHRequest request) {

        // 1️⃣ Validate input
        if (request.getNewSoH() == null) {
            throw new RuntimeException("New SoH value is required");
        }

        if (request.getNewSoH() < 0 || request.getNewSoH() > 100) {
            throw new RuntimeException("SoH must be between 0 and 100");
        }

        // 2️⃣ Find battery
        BatterySerial battery = batterySerialRepository.findById(batteryId)
                .orElseThrow(() -> new RuntimeException("Battery not found with ID: " + batteryId));

        Double oldSoH = Optional.ofNullable(battery.getStateOfHealth()).orElse(100.0);

        // 3️⃣ Find active subscription for this battery's vehicle
        if (battery.getVehicle() == null) {
            throw new RuntimeException(
                    "Battery is not linked to any vehicle. Cannot determine subscription plan constraints.");
        }

        Subscription subscription = subscriptionRepository
                .findByVehicleIdAndStatus(
                        battery.getVehicle().getId(),
                        SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException(
                        "No active subscription found for vehicle ID: " + battery.getVehicle().getId()));

        SubscriptionPlan plan = subscription.getPlan();

        // 4️⃣ Validate new SoH is within plan's range
        Double minSoH = plan.getMinSoH();
        Double maxSoH = plan.getMaxSoH();

        if (minSoH != null && request.getNewSoH() < minSoH) {
            throw new RuntimeException(String.format(
                    "SoH %.1f%% is below plan's minimum %.1f%% (Plan: %s)",
                    request.getNewSoH(), minSoH, plan.getName()));
        }

        if (maxSoH != null && request.getNewSoH() > maxSoH) {
            throw new RuntimeException(String.format(
                    "SoH %.1f%% exceeds plan's maximum %.1f%% (Plan: %s)",
                    request.getNewSoH(), maxSoH, plan.getName()));
        }

        // 5️⃣ Update battery SoH
        battery.setStateOfHealth(request.getNewSoH());
        batterySerialRepository.save(battery);

        log.info(
                "ADMIN UPDATE BATTERY SOH | batteryId={} | serialNumber={} | oldSoH={}% | newSoH={}% | plan={} | range=[{}%, {}%]",
                batteryId, battery.getSerialNumber(), oldSoH, request.getNewSoH(),
                plan.getName(), minSoH, maxSoH);

        // 6️⃣ Build response
        return UpdateBatterySoHResponse.builder()
                .batteryId(batteryId)
                .serialNumber(battery.getSerialNumber())
                .oldSoH(oldSoH)
                .newSoH(request.getNewSoH())
                .planName(plan.getName())
                .planMinSoH(minSoH)
                .planMaxSoH(maxSoH)
                .message(String.format(
                        "Battery SoH updated successfully from %.1f%% to %.1f%%",
                        oldSoH, request.getNewSoH()))
                .build();
    }
}
