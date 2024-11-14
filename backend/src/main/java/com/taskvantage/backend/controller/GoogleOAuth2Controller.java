package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.User;
import com.taskvantage.backend.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GoogleOAuth2Controller {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuth2Controller.class);

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @GetMapping("/oauth2/google/status")
    public ResponseEntity<?> checkGoogleConnectionStatus(HttpServletRequest request) {
        logger.debug("Checking Google connection status");

        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            logger.error("X-User-Id header is missing");
            return ResponseEntity.badRequest().body(Map.of("error", "User ID is required"));
        }

        try {
            User user = userDetailsService.findUserById(Long.parseLong(userId));
            if (user == null) {
                logger.error("User not found with ID: {}", userId);
                return ResponseEntity.ok(Map.of("connected", false));
            }

            // Check if user has valid Google credentials
            boolean isConnected = user.getGoogleAccessToken() != null && user.getGoogleEmail() != null;

            return ResponseEntity.ok(Map.of(
                    "connected", isConnected,
                    "email", user.getGoogleEmail() != null ? user.getGoogleEmail() : ""
            ));
        } catch (Exception e) {
            logger.error("Error checking Google connection status", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
        }
    }
}