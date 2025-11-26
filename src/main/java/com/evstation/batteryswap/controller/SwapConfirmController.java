package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.response.PendingSwapResponse;
import com.evstation.batteryswap.entity.SwapTransaction;
import com.evstation.batteryswap.enums.SwapTransactionStatus;
import com.evstation.batteryswap.repository.SwapTransactionRepository;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.SwapConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/swap")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
public class SwapConfirmController {

        private final SwapConfirmService swapConfirmService;
        private final SwapTransactionRepository swapTransactionRepository;
        private final com.evstation.batteryswap.repository.SubscriptionRepository subscriptionRepository;
        private final com.evstation.batteryswap.repository.BatterySerialRepository batterySerialRepository;
        private final com.evstation.batteryswap.repository.UserRepository userRepository;
        private final com.evstation.batteryswap.repository.ReservationRepository reservationRepository;

        // ✅ Nhân viên xác nhận swap
        @PutMapping("/{transactionId}/confirm")
        public ResponseEntity<String> confirmSwap(
                @PathVariable Long transactionId,
                @RequestBody com.evstation.batteryswap.dto.request.StaffConfirmSwapRequest request,
                @AuthenticationPrincipal CustomUserDetails staff) {

                // Get staff user and verify assigned station
                com.evstation.batteryswap.entity.User staffUser = userRepository.findById(staff.getId())
                        .orElseThrow(() -> new RuntimeException("Staff not found"));

                com.evstation.batteryswap.entity.Station assignedStation = staffUser.getAssignedStation();
                if (assignedStation == null) {
                        throw new RuntimeException("Staff is not assigned to any station");
                }

                // Get transaction and verify it's at staff's station
                SwapTransaction transaction = swapTransactionRepository.findById(transactionId)
                        .orElseThrow(() -> new RuntimeException("Transaction not found"));

                if (!transaction.getStation().getId().equals(assignedStation.getId())) {
                        throw new RuntimeException("Cannot confirm swap at other station. This transaction is at: "
                                + transaction.getStation().getName());
                }

                String result = swapConfirmService.confirmSwap(transactionId, staff.getId(), request);
                return ResponseEntity.ok(result);
        }

        @PutMapping("/{transactionId}/reject")
        public ResponseEntity<String> rejectSwap(
                @PathVariable Long transactionId,
                @AuthenticationPrincipal CustomUserDetails staff) {
                String result = swapConfirmService.rejectSwap(transactionId, staff.getId());
                return ResponseEntity.ok(result);
        }

        // 📋 Xem danh sách swap đang chờ xác nhận (chỉ tại trạm của staff)
        @GetMapping("/pending")
        public ResponseEntity<List<com.evstation.batteryswap.dto.response.PendingSwapResponse>> getPendingSwaps(
                @AuthenticationPrincipal CustomUserDetails staff) {

                // Get staff user and verify assigned station
                com.evstation.batteryswap.entity.User staffUser = userRepository.findById(staff.getId())
                        .orElseThrow(() -> new RuntimeException("Staff not found"));

                com.evstation.batteryswap.entity.Station assignedStation = staffUser.getAssignedStation();
                if (assignedStation == null) {
                        throw new RuntimeException("Staff is not assigned to any station");
                }

                // Only get pending swaps at staff's assigned station
                List<com.evstation.batteryswap.dto.response.PendingSwapResponse> pending = swapTransactionRepository
                        .findByStatus(SwapTransactionStatus.PENDING_CONFIRM)
                        .stream()
                        .filter(tx -> tx.getStation().getId().equals(assignedStation.getId())) // Filter by
                        // staff's
                        // station
                        .map(tx -> {
                                // Lấy subscription để biết SoH range
                                com.evstation.batteryswap.entity.Subscription sub = subscriptionRepository
                                        .findByUserIdAndVehicleIdAndStatus(
                                                tx.getUser().getId(),
                                                tx.getVehicle().getId(),
                                                com.evstation.batteryswap.enums.SubscriptionStatus.ACTIVE)
                                        .orElse(null);

                                // Lấy available batteries tại station, filter theo SoH range
                                List<com.evstation.batteryswap.dto.response.AvailableBatteryInfo> availableBatteries = batterySerialRepository
                                        .findByStationAndStatus(
                                                tx.getStation(),
                                                com.evstation.batteryswap.enums.BatteryStatus.AVAILABLE)
                                        .stream()
                                        .filter(b -> b.getChargePercent() != null
                                                && b.getChargePercent() >= 95.0)
                                        .filter(b -> {
                                                // Filter theo SoH range nếu có subscription
                                                if (sub != null && sub.getPlan().getMinSoH() != null
                                                        && sub.getPlan().getMaxSoH() != null) {
                                                        Double soh = java.util.Optional.ofNullable(
                                                                        b.getStateOfHealth())
                                                                .orElse(100.0);
                                                        return soh >= sub.getPlan().getMinSoH()
                                                                && soh <= sub.getPlan()
                                                                .getMaxSoH();
                                                }
                                                return true; // Nếu không có SoH range thì show all
                                        })
                                        .map(b -> com.evstation.batteryswap.dto.response.AvailableBatteryInfo
                                                .builder()
                                                .id(b.getId())
                                                .serialNumber(b.getSerialNumber())
                                                .chargePercent(b.getChargePercent())
                                                .stateOfHealth(b.getStateOfHealth())
                                                .totalCycleCount(b.getTotalCycleCount())
                                                .batteryModel(b.getBattery().getName()) // Changed
                                                // from
                                                // getModel()
                                                // to
                                                // getName()
                                                .build())
                                        .collect(java.util.stream.Collectors.toList());

                                return com.evstation.batteryswap.dto.response.PendingSwapResponse.builder()
                                        .id(tx.getId())
                                        .username(tx.getUser().getUsername())
                                        .vehicleId(tx.getVehicle().getId())
                                        .vehicleVin(tx.getVehicle().getVin())
                                        .stationName(tx.getStation().getName())
                                        .batterySerialNumber(tx.getBatterySerial().getSerialNumber())
                                        .oldBatterySerialNumber(tx.getBatterySerial().getSerialNumber())
                                        .oldBatteryChargePercent(
                                                tx.getBatterySerial().getChargePercent())
                                        .oldBatterySoH(tx.getBatterySerial().getStateOfHealth())
                                        .availableBatteries(availableBatteries)
                                        .status(tx.getStatus().name())
                                        .timestamp(tx.getTimestamp().toString())
                                        .build();
                        })
                        .toList();

                if (pending.isEmpty())
                        return ResponseEntity.noContent().build();
                return ResponseEntity.ok(pending);
        }

        /**
         * 📦 Lấy danh sách pin từ reservation để staff confirm swap
         * Dùng khi user có đặt lịch trước khi đến đổi pin
         * 
         * @param reservationId ID của reservation
         * @return Danh sách pin đã đặt trong reservation
         */
        @GetMapping("/reservation/{reservationId}/batteries")
        public ResponseEntity<com.evstation.batteryswap.dto.response.ReservationBatteriesResponse> getReservationBatteries(
                @PathVariable Long reservationId,
                @AuthenticationPrincipal CustomUserDetails staff) {

                // Get reservation
                com.evstation.batteryswap.entity.Reservation reservation = reservationRepository
                        .findById(reservationId)
                        .orElseThrow(() -> new RuntimeException("Reservation not found"));

                // Verify staff can access this reservation (same station)
                com.evstation.batteryswap.entity.User staffUser = userRepository.findById(staff.getId())
                        .orElseThrow(() -> new RuntimeException("Staff not found"));

                com.evstation.batteryswap.entity.Station assignedStation = staffUser.getAssignedStation();
                if (assignedStation == null) {
                        throw new RuntimeException("Staff is not assigned to any station");
                }

                if (!reservation.getStation().getId().equals(assignedStation.getId())) {
                        throw new RuntimeException(
                                "Cannot access reservation from other station. This reservation is at: "
                                        + reservation.getStation().getName());
                }

                // Map reservation items to battery info
                List<com.evstation.batteryswap.dto.response.ReservationBatteriesResponse.ReservedBatteryInfo> batteries = reservation
                        .getItems()
                        .stream()
                        .map(item -> {
                                com.evstation.batteryswap.entity.BatterySerial battery = item.getBatterySerial();
                                return com.evstation.batteryswap.dto.response.ReservationBatteriesResponse.ReservedBatteryInfo
                                        .builder()
                                        .batterySerialId(battery.getId())
                                        .serialNumber(battery.getSerialNumber())
                                        .batteryModel(battery.getBattery().getName())
                                        .chargePercent(battery.getChargePercent())
                                        .stateOfHealth(battery.getStateOfHealth())
                                        .totalCycleCount(battery.getTotalCycleCount())
                                        .status(battery.getStatus().name())
                                        .build();
                        })
                        .collect(java.util.stream.Collectors.toList());

                // Build response
                com.evstation.batteryswap.dto.response.ReservationBatteriesResponse response = com.evstation.batteryswap.dto.response.ReservationBatteriesResponse
                        .builder()
                        .reservationId(reservation.getId())
                        .username(reservation.getUser().getUsername())
                        .vehicleVin(reservation.getVehicle().getVin())
                        .stationName(reservation.getStation().getName())
                        .status(reservation.getStatus().name())
                        .quantity(reservation.getQuantity())
                        .usedCount(reservation.getUsedCount())
                        .reservedAt(reservation.getReservedAt())
                        .expireAt(reservation.getExpireAt())
                        .remainingMinutes(reservation.getRemainingMinutes())
                        .batteries(batteries)
                        .build();

                return ResponseEntity.ok(response);
        }

        /**
         * 📦 Lấy danh sách pin từ reservation của một transaction
         * Tiện hơn khi staff đang xem transaction và cần biết có reservation không
         * 
         * @param transactionId ID của swap transaction
         * @return Danh sách pin đã đặt (nếu có reservation), null nếu walk-in
         */
        @GetMapping("/{transactionId}/reservation-batteries")
        public ResponseEntity<?> getReservationBatteriesByTransaction(
                @PathVariable Long transactionId,
                @AuthenticationPrincipal CustomUserDetails staff) {

                // Get transaction
                SwapTransaction transaction = swapTransactionRepository.findById(transactionId)
                        .orElseThrow(() -> new RuntimeException("Transaction not found"));

                // Verify staff can access this transaction (same station)
                com.evstation.batteryswap.entity.User staffUser = userRepository.findById(staff.getId())
                        .orElseThrow(() -> new RuntimeException("Staff not found"));

                com.evstation.batteryswap.entity.Station assignedStation = staffUser.getAssignedStation();
                if (assignedStation == null) {
                        throw new RuntimeException("Staff is not assigned to any station");
                }

                if (!transaction.getStation().getId().equals(assignedStation.getId())) {
                        throw new RuntimeException(
                                "Cannot access transaction from other station. This transaction is at: "
                                        + transaction.getStation().getName());
                }

                // Check if transaction has reservation
                if (transaction.getReservation() == null) {
                        return ResponseEntity.ok(java.util.Map.of(
                                "hasReservation", false,
                                "message", "This is a walk-in swap (no reservation)"));
                }

                com.evstation.batteryswap.entity.Reservation reservation = transaction.getReservation();

                // Map reservation items to battery info
                List<com.evstation.batteryswap.dto.response.ReservationBatteriesResponse.ReservedBatteryInfo> batteries = reservation
                        .getItems()
                        .stream()
                        .map(item -> {
                                com.evstation.batteryswap.entity.BatterySerial battery = item.getBatterySerial();
                                return com.evstation.batteryswap.dto.response.ReservationBatteriesResponse.ReservedBatteryInfo
                                        .builder()
                                        .batterySerialId(battery.getId())
                                        .serialNumber(battery.getSerialNumber())
                                        .batteryModel(battery.getBattery().getName())
                                        .chargePercent(battery.getChargePercent())
                                        .stateOfHealth(battery.getStateOfHealth())
                                        .totalCycleCount(battery.getTotalCycleCount())
                                        .status(battery.getStatus().name())
                                        .build();
                        })
                        .collect(java.util.stream.Collectors.toList());

                // Build response
                com.evstation.batteryswap.dto.response.ReservationBatteriesResponse response = com.evstation.batteryswap.dto.response.ReservationBatteriesResponse
                        .builder()
                        .reservationId(reservation.getId())
                        .username(reservation.getUser().getUsername())
                        .vehicleVin(reservation.getVehicle().getVin())
                        .stationName(reservation.getStation().getName())
                        .status(reservation.getStatus().name())
                        .quantity(reservation.getQuantity())
                        .usedCount(reservation.getUsedCount())
                        .reservedAt(reservation.getReservedAt())
                        .expireAt(reservation.getExpireAt())
                        .remainingMinutes(reservation.getRemainingMinutes())
                        .batteries(batteries)
                        .build();

                return ResponseEntity.ok(java.util.Map.of(
                        "hasReservation", true,
                        "reservation", response));
        }
}
