package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.ReservationRequest;
import com.evstation.batteryswap.dto.response.ReservationResponse;
import com.evstation.batteryswap.entity.*;
import com.evstation.batteryswap.enums.BatteryStatus;
import com.evstation.batteryswap.enums.ReservationStatus;
import com.evstation.batteryswap.enums.SubscriptionStatus;
import com.evstation.batteryswap.repository.*;
import com.evstation.batteryswap.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation của ReservationService
 * 
 * Workflow chính:
 * 1. User tạo reservation → Lock batteries → Save reservation
 * 2. Cron job auto-expire sau 1 giờ
 * 3. Khi swap, mark reservation as USED
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final StationRepository stationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BatterySerialRepository batterySerialRepository;

    /**
     * ========== TẠO RESERVATION ==========
     * 
     * Luồng xử lý:
     * 1. Validate user, vehicle, subscription
     * 2. Check không có reservation ACTIVE cho vehicle này
     * 3. Validate quantity <= plan.maxBatteries
     * 4. Tìm batteries AVAILABLE tại station
     * 5. Lock batteries → RESERVED
     * 6. Tạo Reservation entity
     * 7. Tạo ReservationItems
     * 8. Return response
     */
    @Override
    public ReservationResponse createReservation(Long userId, ReservationRequest request) {
        log.info("CREATE RESERVATION | userId={} | vehicleId={} | stationId={} | quantity={}",
                userId, request.getVehicleId(), request.getStationId(), request.getQuantity());

        // ===== 1. VALIDATE USER =====
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // ===== 2. VALIDATE VEHICLE - Phải thuộc về user =====
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found: " + request.getVehicleId()));

        boolean isOwner = vehicle.getUsers().stream()
                .anyMatch(u -> u.getId().equals(userId));
        if (!isOwner) {
            throw new RuntimeException("Vehicle does not belong to this user");
        }

        // ===== 3. VALIDATE SUBSCRIPTION - User + Vehicle + ACTIVE =====
        Subscription subscription = subscriptionRepository
                .findByUserIdAndVehicleIdAndStatus(userId, request.getVehicleId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription for this vehicle"));

        // ===== 4. CHECK EXISTING ACTIVE RESERVATION - Mỗi vehicle chỉ 1 reservation ACTIVE =====
        boolean hasActiveReservation = reservationRepository
                .existsByUserIdAndVehicleIdAndStatus(userId, request.getVehicleId(), ReservationStatus.ACTIVE);

        if (hasActiveReservation) {
            throw new RuntimeException("This vehicle already has an ACTIVE reservation. Please use or cancel it first.");
        }

        // ===== 5. VALIDATE QUANTITY - Phải <= maxBatteries của plan =====
        int maxBatteries = subscription.getPlan().getMaxBatteries();
        if (request.getQuantity() > maxBatteries) {
            throw new RuntimeException(String.format(
                    "Reservation quantity (%d) exceeds plan limit (%d batteries)",
                    request.getQuantity(), maxBatteries
            ));
        }

        // ===== 6. VALIDATE STATION =====
        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new RuntimeException("Station not found: " + request.getStationId()));

        // ===== 7. FIND & LOCK BATTERIES =====
        List<BatterySerial> batteries = findAndLockBatteries(request, station);

        if (batteries.size() < request.getQuantity()) {
            throw new RuntimeException(String.format(
                    "Not enough AVAILABLE batteries at station %s. Required: %d, Found: %d",
                    station.getName(), request.getQuantity(), batteries.size()
            ));
        }

        // ===== 8. LOCK BATTERIES → RESERVED =====
        batteries.forEach(battery -> battery.setStatus(BatteryStatus.RESERVED));
        batterySerialRepository.saveAll(batteries);

        log.info("BATTERIES LOCKED | stationId={} | count={} | batteries={}",
                station.getId(), batteries.size(),
                batteries.stream().map(BatterySerial::getSerialNumber).collect(Collectors.toList()));

        // ===== 9. CREATE RESERVATION =====
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusHours(1);  // ⏱️ Hết hạn sau 1 giờ

        Reservation reservation = Reservation.builder()
                .user(user)
                .vehicle(vehicle)
                .station(station)
                .subscription(subscription)
                .status(ReservationStatus.ACTIVE)
                .quantity(request.getQuantity())
                .reservedAt(now)
                .expireAt(expireAt)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // ===== 10. CREATE RESERVATION ITEMS =====
        List<ReservationItem> items = batteries.stream()
                .map(battery -> ReservationItem.builder()
                        .reservation(savedReservation)
                        .batterySerial(battery)
                        .build())
                .collect(Collectors.toList());

        reservationItemRepository.saveAll(items);

        log.info("RESERVATION CREATED | reservationId={} | userId={} | vehicleId={} | stationId={} | quantity={} | expireAt={}",
                savedReservation.getId(), userId, vehicle.getId(), station.getId(),
                request.getQuantity(), expireAt);

        // ===== 11. BUILD & RETURN RESPONSE =====
        return buildReservationResponse(savedReservation, batteries);
    }

    /**
     * ========== TÌM VÀ LOCK BATTERIES ==========
     * 
     * Logic:
     * - Nếu user chọn pin cụ thể (batteryIds != null): Validate và lấy pin đó
     * - Nếu không: Auto-select pin tốt nhất (charge >= 95%)
     */
    private List<BatterySerial> findAndLockBatteries(ReservationRequest request, Station station) {
        if (request.getBatteryIds() != null && !request.getBatteryIds().isEmpty()) {
            // === USER CHỌN PIN CỤ THỂ ===
            if (request.getBatteryIds().size() != request.getQuantity()) {
                throw new RuntimeException("Battery IDs count must match quantity");
            }

            List<BatterySerial> batteries = batterySerialRepository.findAllById(request.getBatteryIds());

            // Validate: Pin phải tồn tại, thuộc station, và AVAILABLE
            for (BatterySerial battery : batteries) {
                if (!battery.getStation().getId().equals(station.getId())) {
                    throw new RuntimeException(String.format(
                            "Battery %s does not belong to station %s",
                            battery.getSerialNumber(), station.getName()
                    ));
                }
                if (battery.getStatus() != BatteryStatus.AVAILABLE) {
                    throw new RuntimeException(String.format(
                            "Battery %s is not AVAILABLE (current status: %s)",
                            battery.getSerialNumber(), battery.getStatus()
                    ));
                }
            }

            return batteries;

        } else {
            // === AUTO-SELECT PIN TỐT NHẤT ===
            // Query: SELECT * FROM battery_serials
            //        WHERE station_id = ? AND status = 'AVAILABLE' AND charge_percent >= 95
            //        ORDER BY charge_percent DESC, state_of_health DESC
            //        LIMIT quantity

            List<BatterySerial> availableBatteries = batterySerialRepository
                    .findByStation(station).stream()
                    .filter(b -> b.getStatus() == BatteryStatus.AVAILABLE)
                    .filter(b -> b.getChargePercent() != null && b.getChargePercent() >= 95.0)
                    .sorted((b1, b2) -> {
                        // Ưu tiên: chargePercent DESC, sau đó SoH DESC
                        int chargeCompare = Double.compare(
                                b2.getChargePercent() != null ? b2.getChargePercent() : 0,
                                b1.getChargePercent() != null ? b1.getChargePercent() : 0
                        );
                        if (chargeCompare != 0) return chargeCompare;

                        return Double.compare(
                                b2.getStateOfHealth() != null ? b2.getStateOfHealth() : 0,
                                b1.getStateOfHealth() != null ? b1.getStateOfHealth() : 0
                        );
                    })
                    .limit(request.getQuantity())
                    .collect(Collectors.toList());

            log.info("AUTO-SELECTED BATTERIES | stationId={} | required={} | found={} | batteries={}",
                    station.getId(), request.getQuantity(), availableBatteries.size(),
                    availableBatteries.stream()
                            .map(b -> String.format("%s(%.0f%%/%.0f%%SoH)",
                                    b.getSerialNumber(), b.getChargePercent(), b.getStateOfHealth()))
                            .collect(Collectors.toList()));

            return availableBatteries;
        }
    }

    /**
     * ========== LẤY RESERVATION ACTIVE CỦA VEHICLE ==========
     */
    @Override
    public ReservationResponse getActiveReservation(Long userId, Long vehicleId) {
        return reservationRepository
                .findByUserIdAndVehicleIdAndStatus(userId, vehicleId, ReservationStatus.ACTIVE)
                .map(reservation -> {
                    List<BatterySerial> batteries = reservation.getItems().stream()
                            .map(ReservationItem::getBatterySerial)
                            .collect(Collectors.toList());
                    return buildReservationResponse(reservation, batteries);
                })
                .orElse(null);
    }

    /**
     * ========== LẤY TẤT CẢ RESERVATIONS CỦA USER ==========
     */
    @Override
    public List<ReservationResponse> getUserReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserIdOrderByReservedAtDesc(userId);

        return reservations.stream()
                .map(reservation -> {
                    List<BatterySerial> batteries = reservation.getItems().stream()
                            .map(ReservationItem::getBatterySerial)
                            .collect(Collectors.toList());
                    return buildReservationResponse(reservation, batteries);
                })
                .collect(Collectors.toList());
    }

    /**
     * ========== LẤY CHI TIẾT RESERVATION ==========
     */
    @Override
    public ReservationResponse getReservationById(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithItems(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));

        // Validate ownership
        boolean isOwner = reservation.getVehicle().getUsers().stream()
                .anyMatch(u -> u.getId().equals(userId));
        if (!isOwner) {
            throw new RuntimeException("Reservation does not belong to this user");
        }

        List<BatterySerial> batteries = reservation.getItems().stream()
                .map(ReservationItem::getBatterySerial)
                .collect(Collectors.toList());

        return buildReservationResponse(reservation, batteries);
    }

    /**
     * ========== HỦY RESERVATION ==========
     * 
     * Workflow:
     * 1. Validate ownership và status = ACTIVE
     * 2. Release batteries (RESERVED → AVAILABLE)
     * 3. Update reservation status → CANCELLED
     */
    @Override
    public ReservationResponse cancelReservation(Long userId, Long reservationId, String reason) {
        log.info("CANCEL RESERVATION | userId={} | reservationId={} | reason={}",
                userId, reservationId, reason);

        // ===== 1. VALIDATE RESERVATION =====
        Reservation reservation = reservationRepository.findByIdWithItems(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));

        boolean isOwner = reservation.getVehicle().getUsers().stream()
                .anyMatch(u -> u.getId().equals(userId));
        if (!isOwner) {
            throw new RuntimeException("Reservation does not belong to this user");
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new RuntimeException(String.format(
                    "Cannot cancel reservation with status %s. Only ACTIVE reservations can be cancelled.",
                    reservation.getStatus()
            ));
        }

        // ===== 2. RELEASE BATTERIES → AVAILABLE =====
        List<BatterySerial> batteries = reservation.getItems().stream()
                .map(ReservationItem::getBatterySerial)
                .collect(Collectors.toList());

        batteries.forEach(battery -> battery.setStatus(BatteryStatus.AVAILABLE));
        batterySerialRepository.saveAll(batteries);

        log.info("BATTERIES RELEASED | reservationId={} | count={} | batteries={}",
                reservationId, batteries.size(),
                batteries.stream().map(BatterySerial::getSerialNumber).collect(Collectors.toList()));

        // ===== 3. UPDATE RESERVATION → CANCELLED =====
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancelReason(reason != null ? reason : "User cancelled");
        reservationRepository.save(reservation);

        log.info("RESERVATION CANCELLED | reservationId={} | userId={} | reason={}",
                reservationId, userId, reservation.getCancelReason());

        return buildReservationResponse(reservation, batteries);
    }

    /**
     * ========== CRON JOB: AUTO-EXPIRE RESERVATIONS ==========
     * 
     * Chạy mỗi 1 phút để expire reservations quá hạn
     * 
     * Logic: status = ACTIVE AND expireAt < now()
     */
    @Override
    public void autoExpireReservations() {
        LocalDateTime now = LocalDateTime.now();

        // Tìm reservations đã hết hạn
        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndExpireAtBefore(ReservationStatus.ACTIVE, now);

        if (expiredReservations.isEmpty()) {
            log.debug("AUTO-EXPIRE: No expired reservations found");
            return;
        }

        log.info("AUTO-EXPIRE: Found {} expired reservations", expiredReservations.size());

        for (Reservation reservation : expiredReservations) {
            try {
                // === RELEASE BATTERIES → AVAILABLE ===
                List<BatterySerial> batteries = reservation.getItems().stream()
                        .map(ReservationItem::getBatterySerial)
                        .collect(Collectors.toList());

                batteries.forEach(battery -> battery.setStatus(BatteryStatus.AVAILABLE));
                batterySerialRepository.saveAll(batteries);

                // === UPDATE RESERVATION → EXPIRED ===
                reservation.setStatus(ReservationStatus.EXPIRED);
                reservation.setCancelledAt(now);
                reservation.setCancelReason("Auto-expired after 1 hour");
                reservationRepository.save(reservation);

                log.info("RESERVATION EXPIRED | reservationId={} | userId={} | vehicleId={} | batteries={}",
                        reservation.getId(), reservation.getUser().getId(), reservation.getVehicle().getId(),
                        batteries.stream().map(BatterySerial::getSerialNumber).collect(Collectors.toList()));

            } catch (Exception e) {
                log.error("Failed to expire reservation {} | error: {}",
                        reservation.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * ========== BUILD RESERVATION RESPONSE ==========
     * 
     * Helper method để convert entity sang DTO
     */
    private ReservationResponse buildReservationResponse(Reservation reservation, List<BatterySerial> batteries) {
        String message = switch (reservation.getStatus()) {
            case ACTIVE -> String.format(
                    "Reservation active. Batteries are held for you until %s. Please come to swap within %d minutes.",
                    reservation.getExpireAt(), reservation.getRemainingMinutes()
            );
            case USED -> "Reservation has been used for battery swap.";
            case EXPIRED -> "Reservation has expired. Batteries have been released.";
            case CANCELLED -> "Reservation has been cancelled.";
        };

        return ReservationResponse.builder()
                .reservationId(reservation.getId())
                .status(reservation.getStatus())
                .vehicle(ReservationResponse.VehicleInfo.builder()
                        .id(reservation.getVehicle().getId())
                        .vin(reservation.getVehicle().getVin())
                        .modelName(reservation.getVehicle().getModel().getName())
                        .build())
                .station(ReservationResponse.StationInfo.builder()
                        .id(reservation.getStation().getId())
                        .name(reservation.getStation().getName())
                        .address(reservation.getStation().getLocation())
                        .build())
                .quantity(reservation.getQuantity())
                .batteries(batteries.stream()
                        .map(battery -> ReservationResponse.BatteryInfo.builder()
                                .id(battery.getId())
                                .serialNumber(battery.getSerialNumber())
                                .chargePercent(battery.getChargePercent())
                                .stateOfHealth(battery.getStateOfHealth())
                                .build())
                        .collect(Collectors.toList()))
                .reservedAt(reservation.getReservedAt())
                .expireAt(reservation.getExpireAt())
                .remainingMinutes(reservation.getRemainingMinutes())
                .message(message)
                .swapTransactionId(reservation.getSwapTransactionId())
                .usedAt(reservation.getUsedAt())
                .cancelReason(reservation.getCancelReason())
                .build();
    }
}
