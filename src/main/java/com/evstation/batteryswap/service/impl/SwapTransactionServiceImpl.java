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

                // Lấy user
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Lấy xe
                Vehicle vehicle = vehicleRepository.findById(req.getVehicleId())
                                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
                if (!user.getVehicles().contains(vehicle))
                        throw new RuntimeException("Vehicle does not belong to this user");

                // Kiểm tra subscription ACTIVE
                Subscription sub = subscriptionRepository
                                .findByUserIdAndVehicleIdAndStatus(user.getId(), vehicle.getId(),
                                                SubscriptionStatus.ACTIVE)
                                .orElseThrow(() -> new RuntimeException("No active subscription for this vehicle"));

                // Lấy pin cũ
                BatterySerial oldBattery = batterySerialRepository.findById(req.getBatterySerialId())
                                .orElseThrow(() -> new RuntimeException("Old battery not found"));
                if (oldBattery.getVehicle() == null || !oldBattery.getVehicle().getId().equals(vehicle.getId()))
                        throw new RuntimeException("This battery does not belong to the selected vehicle");
                if (oldBattery.getStatus() != BatteryStatus.IN_USE)
                        throw new RuntimeException("This battery is not currently in use");

                // Lấy trạm
                Station station = stationRepository.findById(req.getStationId())
                                .orElseThrow(() -> new RuntimeException("Station not found"));

                // 🔍 KIỂM TRA RESERVATION (OPTIONAL) - Có thể có hoặc không
                Optional<Reservation> reservationOpt = reservationRepository
                                .findByUserIdAndVehicleIdAndStationIdAndStatus(
                                                user.getId(),
                                                vehicle.getId(),
                                                station.getId(),
                                                ReservationStatus.ACTIVE);

                Reservation reservation = null;
                if (reservationOpt.isPresent()) {
                        reservation = reservationOpt.get();
                        
                        // Kiểm tra reservation còn hiệu lực
                        if (!reservation.isActive()) {
                                throw new RuntimeException("Your reservation has expired. Please create a new reservation.");
                        }

                        log.info("RESERVATION FOUND | reservationId={} | expiresAt={} | batteries=[{}]",
                                        reservation.getId(),
                                        reservation.getExpireAt(),
                                        reservation.getItems().stream()
                                                        .map(item -> item.getBatterySerial().getSerialNumber())
                                                        .collect(Collectors.joining(", ")));
                } else {
                        log.info("NO RESERVATION | Walk-in swap | user={} | vehicle={} | station={}",
                                        user.getUsername(), vehicle.getId(), station.getName());
                }

                // Đặt trạng thái tạm cho pin cũ (chờ staff xác nhận)
                oldBattery.setStatus(BatteryStatus.PENDING_OUT);
                batterySerialRepository.save(oldBattery);

                // Lưu transaction (chưa có thông tin pin mới, chưa tính degradation)
                SwapTransaction tx = SwapTransaction.builder()
                                .user(user)
                                .vehicle(vehicle)
                                .batterySerial(oldBattery)
                                .station(station)
                                .reservation(reservation) // 🔗 Link với reservation (có thể null)
                                .startPercent(Optional.ofNullable(oldBattery.getChargePercent()).orElse(100.0))
                                .timestamp(LocalDateTime.now())
                                .status(SwapTransactionStatus.PENDING_CONFIRM)
                                .build();

                swapTransactionRepository.save(tx);

                // Ghi log
                log.info("SWAP REQUEST | user={} | vehicle={} | oldBattery={} | station={} | reservationId={} | status=PENDING_CONFIRM",
                                user.getUsername(), vehicle.getId(), oldBattery.getSerialNumber(), station.getName(), 
                                reservation != null ? reservation.getId() : "NONE (walk-in)");

                return SwapResponse.builder()
                                .message("Swap request created. Waiting for staff to select battery and confirm at "
                                                + station.getName())
                                .oldSerialNumber(oldBattery.getSerialNumber())
                                .oldSoH(Optional.ofNullable(oldBattery.getStateOfHealth()).orElse(100.0))
                                .status(SwapTransactionStatus.PENDING_CONFIRM.name())
                                .requestedAt(tx.getTimestamp()) // Thời gian gửi yêu cầu
                                .build();
        }

        @Override
        public List<Map<String, Object>> getMostFrequentSwapHour() {
                List<Object[]> results = swapTransactionRepository.findMostFrequentSwapHour();

                return results.stream().map(arr -> {
                        Map<String, Object> map = new HashMap<>();

                        // sửa ở đây:
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

        @Override
        public List<com.evstation.batteryswap.dto.response.SwapHistoryResponse> getUserSwapHistory(String username) {
                // Lấy user
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Lấy tất cả swap transactions của user (COMPLETED và REJECTED)
                List<SwapTransaction> transactions = swapTransactionRepository
                                .findByUserIdOrderByTimestampDesc(user.getId());

                // Convert sang SwapHistoryResponse
                return transactions.stream()
                                .filter(tx -> tx.getStatus() == SwapTransactionStatus.COMPLETED ||
                                                tx.getStatus() == SwapTransactionStatus.REJECTED)
                                .map(tx -> com.evstation.batteryswap.dto.response.SwapHistoryResponse.builder()
                                                .id(tx.getId())
                                                .stationName(tx.getStation().getName())
                                                .oldBatterySerial(tx.getBatterySerial().getSerialNumber())
                                                .newBatterySerial(findNewBatterySerial(tx))
                                                .energyUsed(tx.getEnergyUsed())
                                                .distance(tx.getDistance())
                                                .cost(tx.getCost())
                                                .status(tx.getStatus().name())
                                                .timestamp(tx.getTimestamp())
                                                .confirmedAt(tx.getConfirmedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        private String findNewBatterySerial(SwapTransaction tx) {
                // Tìm pin mới đã được gắn vào xe sau swap
                if (tx.getStatus() == SwapTransactionStatus.COMPLETED) {
                        List<BatterySerial> currentBatteries = batterySerialRepository
                                        .findByVehicleAndStatus(tx.getVehicle(), BatteryStatus.IN_USE);
                        if (!currentBatteries.isEmpty()) {
                                // Lấy pin không phải là pin cũ
                                return currentBatteries.stream()
                                                .filter(b -> !b.getId().equals(tx.getBatterySerial().getId()))
                                                .findFirst()
                                                .map(BatterySerial::getSerialNumber)
                                                .orElse("N/A");
                        }
                }
                return "N/A";
        }
}
