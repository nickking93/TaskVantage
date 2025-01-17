package com.taskvantage.backend.controller;

import com.taskvantage.backend.Security.JwtUtil;
import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.model.Task;
import com.taskvantage.backend.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;

    @Autowired
    public RecommendationController(RecommendationService recommendationService, JwtUtil jwtUtil) {
        this.recommendationService = recommendationService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/user/{userId}/task/{taskId}")
    public ResponseEntity<RecommendationResponse> getRecommendations(
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "3") int limit) {
        try {
            RecommendationResponse response = recommendationService.getRecommendedTasks(userId, taskId, limit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving recommendations for Task ID {}: {}", taskId, e.getMessage());
            RecommendationResponse errorResponse = new RecommendationResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to retrieve recommendations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}