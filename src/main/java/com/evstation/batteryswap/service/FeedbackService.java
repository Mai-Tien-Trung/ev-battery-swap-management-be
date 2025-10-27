package com.evstation.batteryswap.service;

import com.evstation.batteryswap.dto.request.CreateFeedbackRequest;
import com.evstation.batteryswap.dto.response.FeedbackResponse;

import java.util.List;

public interface FeedbackService {

    /**
     * Tạo một feedback mới
     */
    FeedbackResponse createFeedback(CreateFeedbackRequest request);

    /**
     * Lấy tất cả feedback
     */
    List<FeedbackResponse> getAllFeedbacks();

    /**
     * Lấy feedback theo Station ID
     */
    List<FeedbackResponse> getFeedbacksByStationId(Long stationId);

    /**
     * Lấy feedback theo User ID
     */
    List<FeedbackResponse> getFeedbacksByUserId(Long userId);
}