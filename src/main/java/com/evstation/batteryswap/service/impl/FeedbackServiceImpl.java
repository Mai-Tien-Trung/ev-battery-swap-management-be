package com.evstation.batteryswap.service.impl;

import com.evstation.batteryswap.dto.request.CreateFeedbackRequest;
import com.evstation.batteryswap.dto.response.FeedbackResponse;
import lombok.Builder;
import com.evstation.batteryswap.entity.Feedback;
import com.evstation.batteryswap.entity.Station;
import com.evstation.batteryswap.entity.User;
import com.evstation.batteryswap.repository.FeedbackRepository;
import com.evstation.batteryswap.repository.StationRepository;
import com.evstation.batteryswap.repository.UserRepository;
import com.evstation.batteryswap.service.FeedbackService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Tự động inject qua constructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final StationRepository stationRepository;

    @Override
    public FeedbackResponse createFeedback(CreateFeedbackRequest request) {
        // 1. Kiểm tra sự tồn tại của User và Station
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy User với ID: " + request.getUserId()));

        Station station = stationRepository.findById(request.getStationId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Station với ID: " + request.getStationId()));

        // 2. Tạo entity Feedback
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setStation(station);
        feedback.setRating(request.getRating());
        feedback.setContent(request.getContent());

        // 3. Lưu vào DB
        Feedback savedFeedback = feedbackRepository.save(feedback);

        // 4. Map sang DTO để trả về
        return mapToDTO(savedFeedback);
    }

    @Override
    public List<FeedbackResponse> getAllFeedbacks() {
        return feedbackRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeedbackResponse> getFeedbacksByStationId(Long stationId) {
        // Kiểm tra xem station có tồn tại không
        if (!stationRepository.existsById(stationId)) {
            throw new EntityNotFoundException("Không tìm thấy Station với ID: " + stationId);
        }
        return feedbackRepository.findByStationId(stationId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeedbackResponse> getFeedbacksByUserId(Long userId) {
        // Kiểm tra xem user có tồn tại không
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Không tìm thấy User với ID: " + userId);
        }
        return feedbackRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Phương thức private để map từ Entity sang DTO
     */
    private FeedbackResponse mapToDTO(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .rating(feedback.getRating())
                .content(feedback.getContent())
                .createdAt(feedback.getCreatedAt())
                .userId(feedback.getUser().getId())
                .username(feedback.getUser().getUsername()) // Lấy username từ entity User
                .stationId(feedback.getStation().getId())
                .stationName(feedback.getStation().getName()) // Lấy name từ entity Station
                .build();
    }
}