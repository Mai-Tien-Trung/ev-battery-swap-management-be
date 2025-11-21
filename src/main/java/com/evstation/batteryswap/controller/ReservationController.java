package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.CancelReservationRequest;
import com.evstation.batteryswap.dto.request.ReservationRequest;
import com.evstation.batteryswap.dto.response.ReservationResponse;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API cho Battery Reservation
 * 
 * Endpoints:
 * - POST   /api/user/reservations                 → Tạo reservation mới
 * - GET    /api/user/reservations/active          → Lấy reservation ACTIVE của vehicle
 * - GET    /api/user/reservations                 → Lấy tất cả reservations của user
 * - GET    /api/user/reservations/{id}            → Lấy chi tiết reservation
 * - DELETE /api/user/reservations/{id}            → Hủy reservation
 */
@RestController
@RequestMapping("/api/user/reservations")
@RequiredArgsConstructor
@Tag(name = "Battery Reservations", description = "Quản lý đặt trước pin")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * ========== TẠO RESERVATION MỚI ==========
     * 
     * POST /api/user/reservations
     * 
     * Request Body:
     * {
     *   "vehicleId": 1,
     *   "stationId": 5,
     *   "quantity": 2,
     *   "batteryIds": [101, 102]  // Optional - auto-select if null
     * }
     * 
     * Response:
     * - 201 Created: Reservation đã được tạo thành công
     * - 400 Bad Request: Validation errors (quantity > maxBatteries, vehicle đã có reservation ACTIVE, etc.)
     * - 404 Not Found: Vehicle/Station/Subscription không tồn tại
     */
    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Tạo reservation mới",
            description = "Đặt trước pin tại trạm. Hệ thống sẽ lock pin trong 1 giờ.")
    public ResponseEntity<ReservationResponse> createReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReservationRequest request) {

        Long userId = userDetails.getId();

        log.info("API: CREATE RESERVATION | userId={} | request={}", userId, request);

        ReservationResponse response = reservationService.createReservation(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ========== LẤY RESERVATION ACTIVE CỦA VEHICLE ==========
     * 
     * GET /api/user/reservations/active?vehicleId=1
     * 
     * Response:
     * - 200 OK: Trả về reservation ACTIVE của vehicle
     * - 204 No Content: Vehicle không có reservation ACTIVE
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Lấy reservation ACTIVE của vehicle",
            description = "Check xem xe có reservation đang chờ hay không")
    public ResponseEntity<ReservationResponse> getActiveReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long vehicleId) {

        Long userId = userDetails.getId();

        log.info("API: GET ACTIVE RESERVATION | userId={} | vehicleId={}", userId, vehicleId);

        ReservationResponse response = reservationService.getActiveReservation(userId, vehicleId);

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * ========== LẤY TẤT CẢ RESERVATIONS CỦA USER ==========
     * 
     * GET /api/user/reservations
     * 
     * Response:
     * - 200 OK: Danh sách reservations (ACTIVE, USED, EXPIRED, CANCELLED)
     * - 200 OK (empty array): User chưa có reservation nào
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Lấy lịch sử reservations của user",
            description = "Trả về tất cả reservations (bao gồm cả USED, EXPIRED, CANCELLED)")
    public ResponseEntity<List<ReservationResponse>> getUserReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();

        log.info("API: GET USER RESERVATIONS | userId={}", userId);

        List<ReservationResponse> reservations = reservationService.getUserReservations(userId);

        return ResponseEntity.ok(reservations);
    }

    /**
     * ========== LẤY CHI TIẾT RESERVATION ==========
     * 
     * GET /api/user/reservations/{id}
     * 
     * Response:
     * - 200 OK: Chi tiết reservation
     * - 404 Not Found: Reservation không tồn tại hoặc không thuộc user
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Lấy chi tiết reservation",
            description = "Xem thông tin đầy đủ của 1 reservation (vehicle, station, batteries, etc.)")
    public ResponseEntity<ReservationResponse> getReservationById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {

        Long userId = userDetails.getId();

        log.info("API: GET RESERVATION DETAIL | userId={} | reservationId={}", userId, id);

        ReservationResponse response = reservationService.getReservationById(userId, id);

        return ResponseEntity.ok(response);
    }

    /**
     * ========== HỦY RESERVATION ==========
     * 
     * DELETE /api/user/reservations/{id}
     * 
     * Request Body:
     * {
     *   "reason": "Không đến được trạm" // Optional
     * }
     * 
     * Response:
     * - 200 OK: Reservation đã được hủy, batteries được release
     * - 400 Bad Request: Không thể hủy (chỉ hủy được ACTIVE)
     * - 404 Not Found: Reservation không tồn tại
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER')")
    @Operation(summary = "Hủy reservation",
            description = "Hủy reservation ACTIVE. Batteries sẽ được release về AVAILABLE.")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) CancelReservationRequest request) {

        Long userId = userDetails.getId();
        String reason = (request != null && request.getReason() != null)
                ? request.getReason()
                : "User cancelled";

        log.info("API: CANCEL RESERVATION | userId={} | reservationId={} | reason={}",
                userId, id, reason);

        ReservationResponse response = reservationService.cancelReservation(userId, id, reason);

        return ResponseEntity.ok(response);
    }
}
