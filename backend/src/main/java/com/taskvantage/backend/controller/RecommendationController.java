package com.taskvantage.backend.controller;

import com.taskvantage.backend.Security.AuthorizationUtil;
import com.taskvantage.backend.Security.JwtUtil;
import com.taskvantage.backend.dto.RecommendationResponse;
import com.taskvantage.backend.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;
    private final AuthorizationUtil authorizationUtil;

    @Autowired
    public RecommendationController(RecommendationService recommendationService, JwtUtil jwtUtil, AuthorizationUtil authorizationUtil) {
        this.recommendationService = recommendationService;
        this.jwtUtil = jwtUtil;
        this.authorizationUtil = authorizationUtil;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getRecommendationsForUser(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") int limit) {
        // Validate that the authenticated user matches the requested userId
        ResponseEntity<Map<String, Object>> authError = authorizationUtil.validateUserAccess(authorizationHeader, userId);
        if (authError != null) {
            return authError;
        }

        try {
            RecommendationResponse response = recommendationService.getRecommendedTasks(userId, null, limit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving recommendations for user {}: {}", userId, e.getMessage());
            RecommendationResponse errorResponse = new RecommendationResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to retrieve recommendations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/user/{userId}/task/{taskId}")
    public ResponseEntity<?> getRecommendationsForTask(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long userId,
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "3") int limit) {
        // Validate that the authenticated user matches the requested userId
        ResponseEntity<Map<String, Object>> authError = authorizationUtil.validateUserAccess(authorizationHeader, userId);
        if (authError != null) {
            return authError;
        }

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

    @GetMapping("/user/{userId}/weekday")
    public ResponseEntity<?> getWeekdayRecommendations(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "3") int limit) {
        // Validate that the authenticated user matches the requested userId
        ResponseEntity<Map<String, Object>> authError = authorizationUtil.validateUserAccess(authorizationHeader, userId);
        if (authError != null) {
            return authError;
        }

        try {
            RecommendationResponse response = recommendationService.getRecommendedTasksByWeekday(userId, limit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving weekday recommendations for user {}: {}", userId, e.getMessage());
            RecommendationResponse errorResponse = new RecommendationResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Failed to retrieve recommendations");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}