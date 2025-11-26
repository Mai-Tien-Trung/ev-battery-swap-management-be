package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.response.ReputationResponse;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.ReputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API cho Reputation System
 * 
 * Endpoint:
 * - GET /api/user/reputation → Lấy thông tin uy tín của user
 */
@RestController
@RequestMapping("/api/user/reputation")
@RequiredArgsConstructor
@Tag(name = "Reputation", description = "Quản lý uy tín đặt lịch")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class ReputationController {

    private final ReputationService reputationService;

    /**
     * ========== LẤY UY TÍN CỦA USER ==========
     * 
     * GET /api/user/reputation
     * 
     * Response:
     * {
     *   "currentReputation": 4,
     *   "maxReputation": 6,
     *   "cancelledCount": 1,
     *   "expiredCount": 0,
     *   "usedCount": 3,
     *   "canReserve": true,
     *   "message": "Uy tín tốt: 4/6 điểm. Bạn có thể đặt lịch bình thường."
     * }
     * 
     * Quy tắc:
     * - Điểm tối đa: 6/tháng
     * - Hủy reservation: -1 điểm
     * - Hết hạn (expired): -2 điểm
     * - Điểm <= 0: KHÔNG được đặt lịch
     */
    @GetMapping
    @PreAuthorize("hasAuthority('USER')")
    @Operation(
        summary = "Lấy thông tin uy tín đặt lịch",
        description = "Hiển thị điểm uy tín hiện tại và lịch sử reservations trong tháng. " +
                      "Điểm uy tín quyết định khả năng đặt lịch của user."
    )
    public ResponseEntity<ReputationResponse> getMyReputation(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getId();
        
        log.info("API: GET USER REPUTATION | userId={}", userId);
        
        ReputationResponse reputation = reputationService.getUserReputation(userId);
        
        return ResponseEntity.ok(reputation);
    }
}
