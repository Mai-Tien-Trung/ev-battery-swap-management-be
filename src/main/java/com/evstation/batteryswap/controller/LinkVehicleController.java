package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.LinkVehicleRequest;
import com.evstation.batteryswap.dto.response.LinkVehicleResponse;
import com.evstation.batteryswap.security.CustomUserDetails;
import com.evstation.batteryswap.service.LinkVehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/user")
public class LinkVehicleController {

    @Autowired
    private LinkVehicleService linkVehicleService;

    @PostMapping("/link-vehicle")
    public ResponseEntity<LinkVehicleResponse> linkVehicle(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody LinkVehicleRequest request) {

        Long userId = userDetails.getId(); // lấy từ token
        LinkVehicleResponse response = linkVehicleService.linkVehicle(userId, request);
        return ResponseEntity.ok(response);
    }
}
