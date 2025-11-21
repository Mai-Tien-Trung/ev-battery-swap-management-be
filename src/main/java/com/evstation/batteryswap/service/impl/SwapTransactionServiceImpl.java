package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.SwapRequest;
import com.evstation.batteryswap.dto.response.SwapResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.PlanType;
import com.evstation.batteryswap.enums.ReservationStatus;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.SwapTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SwapTransactionServiceImpl implements SwapTransactionService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final BatterySerialRepository batterySerialRepository;
    private final StationRepository stationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanTierRateRepository planTierRateRepository;
    private final SwapTransactionRepository swapTransactionRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;

    @Override
    public SwapResponse processSwap(String username, SwapRequest req) {

        //  Lấy user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Lấy xe
        Vehicle vehicle = vehicleRepository.findById(req.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        if (!user.getVehicles().contains(vehicle))
            throw new RuntimeException("Vehicle does not belong to this user");

        //  Kiểm tra subscription ACTIVE
        Subscription sub = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(user.getId(), vehicle.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription for this vehicle"));
        PlanType planType = sub.getPlan().getPlanType();

        //  Lấy pin cũ
        BatterySerial oldBattery = batterySerialRepository.findById(req.getBatterySerialId())
                .orElseThrow(() -> new RuntimeException("Old battery not found"));
        if (oldBattery.getVehicle() == null || !oldBattery.getVehicle().getId().equals(vehicle.getId()))
            throw new RuntimeException("This battery does not belong to the selected vehicle");
        if (oldBattery.getStatus() != BatteryStatus.IN_USE)
            throw new RuntimeException("This battery is not currently in use");

        // ⃣Lấy trạm
        Station station = stationRepository.findById(req.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found"));

        // ===== CHECK RESERVATION - Ưu tiên pin đã đặt trước =====
        BatterySerial newBattery = null;
        Reservation activeReservation = null;

        // 1. Tìm reservation ACTIVE của vehicle tại station này
        Optional<Reservation> reservationOpt = reservationRepository
                .findByUserIdAndVehicleIdAndStationIdAndStatus(
                        user.getId(),
                        vehicle.getId(),
                        station.getId(),
                        ReservationStatus.ACTIVE
                );

        if (reservationOpt.isPresent()) {
            activeReservation = reservationOpt.get();
            
            // 2. Lấy pin TỪ RESERVATION (status = RESERVED)
            List<BatterySerial> reservedBatteries = activeReservation.getItems().stream()
                    .map(ReservationItem::getBatterySerial)
                    .filter(b -> b.getStatus() == BatteryStatus.RESERVED)
                    .filter(b -> b.getChargePercent() != null && b.getChargePercent() >= 95.0)
                    .sorted((b1, b2) -> Double.compare(
                            b2.getChargePercent() != null ? b2.getChargePercent() : 0,
                            b1.getChargePercent() != null ? b1.getChargePercent() : 0
                    ))
                    .toList();

            if (!reservedBatteries.isEmpty()) {
                BatterySerial selectedBattery = reservedBatteries.get(0);  // Lấy pin tốt nhất từ reservation
                
                // ✅ VALIDATE: Pin này PHẢI thuộc reservation items
                final Long selectedBatteryId = selectedBattery.getId();
                boolean isBatteryInReservation = activeReservation.getItems().stream()
                        .anyMatch(item -> item.getBatterySerial().getId().equals(selectedBatteryId));
                
                if (!isBatteryInReservation) {
                    log.error("CRITICAL ERROR: Selected battery NOT in reservation items | battery={} | reservationId={}",
                            selectedBattery.getSerialNumber(), activeReservation.getId());
                    throw new RuntimeException("Internal error: Selected battery does not match reservation");
                }
                
                newBattery = selectedBattery;  // Gán vào biến newBattery
                
                log.info("SWAP WITH RESERVATION | reservationId={} | battery={} | charge={}% | VALIDATED=true",
                        activeReservation.getId(), newBattery.getSerialNumber(), newBattery.getChargePercent());
            } else {
                log.warn("RESERVATION EXISTS but no suitable RESERVED batteries | reservationId={} | itemsCount={}",
                        activeReservation.getId(), activeReservation.getItems().size());
            }
        }

        // 3. Nếu KHÔNG có reservation hoặc không có pin phù hợp → Lấy pin AVAILABLE ngẫu nhiên
        if (newBattery == null) {
            newBattery = batterySerialRepository
                    .findRandomAvailableBatteryAtStation(station.getId())
                    .orElseThrow(() -> new RuntimeException("No available battery at this station"));
            
            log.info("SWAP WITHOUT RESERVATION | battery={} | charge={}%",
                    newBattery.getSerialNumber(), newBattery.getChargePercent());
        }

        double newBatteryPercent = Optional.ofNullable(newBattery.getChargePercent()).orElse(100.0);
        if (newBatteryPercent < 95)
            throw new RuntimeException("No fully charged battery available at this station");

        // Tính toán năng lượng hao mòn pin cũ
        double designCapacityWh = oldBattery.getBattery().getDesignCapacity();
        double startPercent = Optional.ofNullable(oldBattery.getChargePercent()).orElse(100.0);
        double endPercent = req.getEndPercent();
        double depth = Math.max(0, startPercent - endPercent);
        double energyUsedWh = (depth / 100.0) * designCapacityWh;
        double energyUsedKWh = energyUsedWh / 1000.0;
        double cycleUsed = depth / 100.0;
        double degradation = cycleUsed * 0.75;
        double oldSoH = Optional.ofNullable(oldBattery.getStateOfHealth()).orElse(100.0);
        double newSoH = Math.max(0, oldSoH - degradation);
        oldBattery.setStateOfHealth(newSoH);
        oldBattery.setTotalCycleCount(
                Optional.ofNullable(oldBattery.getTotalCycleCount()).orElse(0.0) + cycleUsed
        );

        double efficiencyKmPerKwh = Optional.ofNullable(vehicle.getEfficiencyKmPerKwh()).orElse(20.0);
        double distanceTraveled = energyUsedKWh * efficiencyKmPerKwh;

        // Đặt trạng thái tạm (pending)
        oldBattery.setStatus(BatteryStatus.PENDING_OUT);
        newBattery.setStatus(BatteryStatus.PENDING_IN);
        batterySerialRepository.saveAll(java.util.List.of(oldBattery, newBattery));

        //  Lưu transaction (chưa đổi pin thực tế)
        SwapTransaction tx = SwapTransaction.builder()
                .user(user)
                .vehicle(vehicle)
                .batterySerial(oldBattery)
                .station(station)
                .energyUsed(energyUsedKWh)
                .distance(distanceTraveled)
                .cost(0.0)
                .startPercent(startPercent)
                .endPercent(endPercent)
                .depthOfDischarge(depth)
                .degradationThisSwap(degradation)
                .timestamp(LocalDateTime.now())
                .status(SwapTransactionStatus.PENDING_CONFIRM)
                .build();

        swapTransactionRepository.save(tx);

        // ===== MARK RESERVATION AS USED (nếu swap dùng pin từ reservation) =====
        if (activeReservation != null) {
            activeReservation.setStatus(ReservationStatus.USED);
            activeReservation.setUsedAt(LocalDateTime.now());
            activeReservation.setSwapTransactionId(tx.getId());
            reservationRepository.save(activeReservation);

            log.info("RESERVATION USED IN SWAP | reservationId={} | swapTxId={} | battery={}",
                    activeReservation.getId(), tx.getId(), newBattery.getSerialNumber());
        }

        // Ghi log & trả response
        log.info("SWAP REQUEST | user={} | vehicle={} | oldBattery={} | newBattery={} | hasReservation={} | status=PENDING_CONFIRM",
                user.getUsername(), vehicle.getId(), oldBattery.getSerialNumber(), 
                newBattery.getSerialNumber(), activeReservation != null);

        return SwapResponse.builder()
                .message("Swap request created. Waiting for staff confirmation at " + station.getName())
                .oldSerialNumber(oldBattery.getSerialNumber())
                .newSerialNumber(newBattery.getSerialNumber())
                .oldSoH(oldSoH)
                .newSoH(newSoH)
                .depthOfDischarge(depth)
                .energyUsed(energyUsedKWh)
                .distanceUsed(distanceTraveled)
                .status(SwapTransactionStatus.PENDING_CONFIRM.name())
                .build();
    }
    @Override
    public List<Map<String, Object>> getMostFrequentSwapHour() {
        List<Object[]> results = swapTransactionRepository.findMostFrequentSwapHour();

        return results.stream().map(arr -> {
            Map<String, Object> map = new HashMap<>();

            //  sửa ở đây:
            BigDecimal hourBigDecimal = (BigDecimal) arr[0];
            Long count = (Long) arr[1];

            map.put("hour", hourBigDecimal.intValue()); // Giờ
            map.put("count", count);
            return map;
        }).collect(Collectors.toList());
    }
    @Override
    public List<Map<String, Object>> getSwapsPerStation() {
        List<Object[]> results = swapTransactionRepository.findSwapsPerStation();
        return results.stream().map(arr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("stationName", (String) arr[0]);
            map.put("swapCount", (Long) arr[1]);
            return map;
        }).collect(Collectors.toList());
    }
}
