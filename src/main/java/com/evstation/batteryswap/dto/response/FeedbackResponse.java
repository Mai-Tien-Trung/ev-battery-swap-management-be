package com.evstation.batteryswap.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@Builder
public class FeedbackResponse {



    private Long id;
    private int rating;
    private String content;
    private LocalDateTime createdAt;

    // Thông tin rút gọn của User và Station
    private Long userId;
    private String username;
    private Long stationId;
    private String stationName;
}