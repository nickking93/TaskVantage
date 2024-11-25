package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.AuthRequest;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.service.CustomUserDetailsService;
import com.taskvantage.backend.Security.JwtUtil;
import com.taskvantage.backend.service.FirebaseNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FirebaseNotificationService firebaseNotificationService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest authRequest) {
        logger.info("Login request received for user: {}", authRequest.getUsername());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            // Load user details
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(authRequest.getUsername());
            User user = customUserDetailsService.findUserByUsername(authRequest.getUsername());

            // Check if email is verified
            if (!user.isEmailVerified()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Your email is not verified. Please verify your email before logging in.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Check if this is a PWA request
            boolean isPwa = authRequest.getIsPwa() != null && authRequest.getIsPwa();

            // Generate tokens
            Map<String, String> tokens = jwtUtil.generateTokens(userDetails, user.getId(), isPwa);

            // Store refresh token
            user.setRefreshToken(tokens.get("refreshToken"));
            customUserDetailsService.saveUser(user);

            // Handle FCM token if provided
            if (authRequest.getFcmToken() != null && !authRequest.getFcmToken().isEmpty()) {
                firebaseNotificationService.sendNotification(
                        authRequest.getFcmToken(),
                        "Welcome to TaskVantage",
                        "Thank you for logging in. Enjoy using the app!",
                        user.getUsername()
                );
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", user.getUsername());
            response.put("userId", user.getId());
            response.put("token", tokens.get("accessToken"));
            response.put("refreshToken", tokens.get("refreshToken"));
            response.put("isPwa", isPwa);

            logger.info("Login successful for user: {}", user.getUsername());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            logger.error("Login failed for user: {}. Error: {}", authRequest.getUsername(), e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest authRequest) {
        String result = customUserDetailsService.registerUser(authRequest);
        Map<String, Object> response = new HashMap<>();

        if (result.equals("Registration successful. Please check your email to verify your account.")) {
            response.put("message", result);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", result);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        logger.info("Token refresh request received");
        Map<String, Object> response = new HashMap<>();

        try {
            if (refreshToken == null || !refreshToken.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Invalid refresh token format");
            }

            String token = refreshToken.substring(7);
            if (!jwtUtil.isRefreshToken(token)) {
                throw new IllegalArgumentException("Token is not a refresh token");
            }

            String username = jwtUtil.getUsernameFromToken(token);
            User user = customUserDetailsService.findUserByUsername(username);

            if (!token.equals(user.getRefreshToken())) {
                throw new IllegalArgumentException("Invalid refresh token");
            }

            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            String newAccessToken = jwtUtil.generateTokenFromRefreshToken(token, userDetails);

            response.put("token", newAccessToken);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            response.put("message", "Token refresh failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String token) {
        logger.info("Logout request received");
        Map<String, Object> response = new HashMap<>();

        try {
            if (token != null && token.startsWith("Bearer ")) {
                String jwtToken = token.substring(7);
                String username = jwtUtil.getUsernameFromToken(jwtToken);
                User user = customUserDetailsService.findUserByUsername(username);

                // Clear refresh token
                user.setRefreshToken(null);
                customUserDetailsService.saveUser(user);

                response.put("message", "Logout successful");
                return ResponseEntity.ok(response);
            }

            throw new IllegalArgumentException("Invalid token format");
        } catch (Exception e) {
            logger.error("Logout failed: {}", e.getMessage());
            response.put("message", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/users/{username}/clear-fcm-token")
    public ResponseEntity<?> clearFcmToken(@PathVariable("username") String username) {
        try {
            customUserDetailsService.clearUserToken(username);
            return ResponseEntity.ok("FCM Token cleared successfully.");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + username);
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("token") String token) {
        try {
            User user = customUserDetailsService.findUserByVerificationToken(token);

            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid verification token.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            user.setEmailVerified(true);
            user.setVerificationToken(null);
            customUserDetailsService.saveUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Email successfully verified.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error while verifying email", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred while verifying the email.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> sendResetPasswordLink(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");

        Map<String, Object> response = new HashMap<>();

        if (email == null || email.isEmpty()) {
            response.put("message", "Email is required.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            boolean resetLinkSent = customUserDetailsService.sendPasswordResetLink(email);

            if (resetLinkSent) {
                response.put("message", "Password reset link sent successfully.");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Unable to send reset link. Please verify the email address.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (UsernameNotFoundException e) {
            response.put("message", "Email address not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("Error while sending password reset link", e);
            response.put("message", "An error occurred while sending the reset link.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> requestBody) {
        String token = requestBody.get("token");
        String newPassword = requestBody.get("newPassword");

        Map<String, Object> response = new HashMap<>();

        if (token == null || token.isEmpty()) {
            response.put("message", "Reset token is required.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (newPassword == null || newPassword.isEmpty()) {
            response.put("message", "New password is required.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        try {
            boolean passwordUpdated = customUserDetailsService.updatePassword(token, newPassword);

            if (passwordUpdated) {
                response.put("message", "Password updated successfully.");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Invalid or expired reset token.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } catch (Exception e) {
            logger.error("Error while resetting password", e);
            response.put("message", "An error occurred while resetting the password.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/update-fcm-token")
    public ResponseEntity<Map<String, Object>> updateFCMToken(@RequestBody Map<String, String> request, Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated()) {
            response.put("message", "User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String fcmToken = request.get("fcmToken");
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            response.put("message", "FCM token is required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String username = authentication.getName();
            customUserDetailsService.updateUserToken(username, fcmToken);

            response.put("message", "FCM token updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error updating FCM token", e);
            response.put("message", "Failed to update FCM token");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/user/settings")
    public ResponseEntity<Map<String, Object>> getUserSettings(Authentication principal) {
        User user = customUserDetailsService.findUserByUsername(principal.getName());
        Map<String, Object> settings = new HashMap<>();

        settings.put("googleConnected", user.getGoogleAccessToken() != null);
        settings.put("taskSyncEnabled", user.isTaskSyncEnabled());

        return ResponseEntity.ok(settings);
    }
}