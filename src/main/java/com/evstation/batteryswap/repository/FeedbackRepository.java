package com.evstation.batteryswap.repository;

import com.evstation.batteryswap.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Tìm feedback theo station id
    List<Feedback> findByStationId(Long stationId);

    // Tìm feedback theo user id
    List<Feedback> findByUserId(Long userId);
}