package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.SwapTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SwapTransactionServiceImpl implements SwapTransactionService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final BatterySerialRepository batterySerialRepository;
    private final StationRepository stationRepository;
    private final SwapTransactionRepository swapTransactionRepository;

    @Override
    public SwapResponse processSwap(String username, SwapRequest req) {
        // 1 Lấy user từ token
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2 Xác định vehicle
        Vehicle vehicle;
        if (req.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(req.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found"));
            if (!user.getVehicles().contains(vehicle)) {
                throw new RuntimeException("Vehicle does not belong to this user");
            }
        } else {
            if (user.getVehicles().isEmpty()) {
                throw new RuntimeException("User has no linked vehicles");
            } else if (user.getVehicles().size() > 1) {
                throw new RuntimeException("Multiple vehicles found. Please specify vehicleId.");
            }
            vehicle = user.getVehicles().get(0);
        }

        // 3 Tìm pin đang IN_USE của xe đó
        BatterySerial oldBattery = batterySerialRepository
                .findFirstByVehicleAndStatus(vehicle, BatteryStatus.IN_USE)
                .orElseThrow(() -> new RuntimeException("No battery currently in use for this vehicle"));

        // 4 Lấy startPercent từ lần swap gần nhất của pin đó
        SwapTransaction lastTx = swapTransactionRepository
                .findTopByBatterySerialOrderByTimestampDesc(oldBattery)
                .orElseThrow(() -> new RuntimeException("No previous transaction for this battery"));

        double startPercent = lastTx.getStartPercent();
        double endPercent = req.getEndPercent();

        if (endPercent >= startPercent) {
            throw new IllegalArgumentException("endPercent must be less than startPercent");
        }

        // 5️⃣ Tính hao mòn
        double depth = startPercent - endPercent;
        double cycleUsed = depth / 100.0;
        double degradation = cycleUsed * 0.75;

        double oldSoH = oldBattery.getStateOfHealth();
        double newSoH = Math.max(0, oldSoH - degradation);

        oldBattery.setStateOfHealth(newSoH);
        oldBattery.setTotalCycleCount(oldBattery.getTotalCycleCount() + cycleUsed);

        // 6️⃣ Cập nhật trạng thái pin cũ
        Station station = stationRepository.findById(req.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        if (newSoH < 80) oldBattery.setStatus(BatteryStatus.MAINTENANCE);
        else oldBattery.setStatus(BatteryStatus.AVAILABLE);

        oldBattery.setVehicle(null);
        oldBattery.setStation(station);
        batterySerialRepository.save(oldBattery);

        // 7️⃣ Cấp pin mới (random 95–100%)
        BatterySerial newBattery = batterySerialRepository
                .findFirstByStationAndStatus(station, BatteryStatus.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("No available battery in this station"));

        double randomStartPercent = 95 + new Random().nextDouble() * 5;
        newBattery.setStatus(BatteryStatus.IN_USE);
        newBattery.setVehicle(vehicle);
        newBattery.setStation(null);
        batterySerialRepository.save(newBattery);

        // 8️ Log transaction
        double energyUsed = (depth / 100.0) * oldBattery.getBattery().getDesignCapacity();
        double cost = energyUsed * 1000;

        SwapTransaction tx = SwapTransaction.builder()
                .user(user)
                .vehicle(vehicle)
                .batterySerial(oldBattery)
                .station(station)
                .startPercent(randomStartPercent)
                .endPercent(endPercent)
                .depthOfDischarge(depth)
                .degradationThisSwap(degradation)
                .energyUsed(energyUsed)
                .cost(cost)
                .timestamp(LocalDateTime.now())
                .build();
        swapTransactionRepository.save(tx);

        log.info(" User {} swapped on vehicle {} | old {} → new {} | SoH: {}→{} | Station {}",
                user.getUsername(),
                vehicle.getVin(),
                oldBattery.getSerialNumber(),
                newBattery.getSerialNumber(),
                String.format("%.2f", oldSoH),
                String.format("%.2f", newSoH),
                station.getName()
        );

        // 9️ Trả response
        return SwapResponse.builder()
                .message("Swap completed successfully at station " + station.getName())
                .oldSerialNumber(oldBattery.getSerialNumber())
                .newSerialNumber(newBattery.getSerialNumber())
                .oldSoH(oldSoH)
                .newSoH(newSoH)
                .depthOfDischarge(depth)
                .degradationThisSwap(degradation)
                .totalCycleCount(oldBattery.getTotalCycleCount())
                .energyUsed(energyUsed)
                .cost(cost)
                .status(oldBattery.getStatus())
                .build();
    }
}