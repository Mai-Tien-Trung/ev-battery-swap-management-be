package com.evstation.batteryswap.controller;

import com.evstation.batteryswap.dto.request.CreateFeedbackRequest;
import com.evstation.batteryswap.dto.response.FeedbackResponse;
import com.evstation.batteryswap.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedbacks") // Cấu trúc URL chuẩn REST
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    // 4. Tạo feedback
    @PostMapping
    public ResponseEntity<FeedbackResponse> createFeedback(@Valid @RequestBody CreateFeedbackRequest request) {
        FeedbackResponse createdFeedback = feedbackService.createFeedback(request);
        return new ResponseEntity<>(createdFeedback, HttpStatus.CREATED);
    }

    // 1. Get tất cả feedbacks
    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getAllFeedbacks() {
        List<FeedbackResponse> feedbacks = feedbackService.getAllFeedbacks();
        return ResponseEntity.ok(feedbacks);
    }

    // 2. Get feedback theo station id
    @GetMapping("/station/{stationId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByStation(@PathVariable Long stationId) {
        List<FeedbackResponse> feedbacks = feedbackService.getFeedbacksByStationId(stationId);
        return ResponseEntity.ok(feedbacks);
    }

    // 3. Get feedback theo user id
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FeedbackResponse>> getFeedbacksByUser(@PathVariable Long userId) {
        List<FeedbackResponse> feedbacks = feedbackService.getFeedbacksByUserId(userId);
        return ResponseEntity.ok(feedbacks);
    }
}