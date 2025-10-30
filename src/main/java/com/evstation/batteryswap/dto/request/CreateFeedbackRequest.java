package com.evstation.batteryswap.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateFeedbackRequest {

    @NotNull(message = "User ID không được để trống")
    private Long userId;

    @NotNull(message = "Station ID không được để trống")
    private Long stationId;

    @NotNull(message = "Đánh giá sao không được để trống")
    @Min(value = 1, message = "Đánh giá phải từ 1 đến 5 sao")
    @Max(value = 5, message = "Đánh giá phải từ 1 đến 5 sao")
    private Integer rating;

    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 5, max = 1000, message = "Nội dung phải từ 5 đến 1000 ký tự")
    private String content;
}