package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.AuthRequest;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.service.CustomUserDetailsService;
import com.taskvantage.backend.Security.JwtUtil;
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

    /**
     * Authenticates the user and generates a JWT token if successful.
     *
     * @param authRequest Contains the username and password.
     * @return A response entity with the login result and JWT token.
     */
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

            // Generate JWT token
            String token = jwtUtil.generateToken(userDetails, user.getId());

            // Handle FCM token if provided
            if (authRequest.getFcmToken() != null && !authRequest.getFcmToken().isEmpty()) {
                customUserDetailsService.updateUserToken(user.getUsername(), authRequest.getFcmToken());
                logger.info("Updated FCM Token for user: {}", user.getUsername());
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", user.getUsername());
            response.put("userId", user.getId());
            response.put("token", token);

            logger.info("Login successful for user: {}", user.getUsername());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            logger.error("Login failed for user: {}. Error: {}", authRequest.getUsername(), e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * Registers a new user and sends a verification email.
     *
     * @param authRequest Contains the username and password for registration.
     * @return A response entity with the registration result.
     */
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

    /**
     * Clears the FCM (Firebase Cloud Messaging) token for a user.
     *
     * @param username The username of the user whose FCM token should be cleared.
     * @return A response entity with the result.
     */
    @PostMapping("/users/{username}/clear-fcm-token")
    public ResponseEntity<?> clearFcmToken(@PathVariable("username") String username) {
        try {
            customUserDetailsService.clearUserToken(username);
            return ResponseEntity.ok("FCM Token cleared successfully.");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + username);
        }
    }

    /**
     * Verifies a user's email using a verification token.
     *
     * @param token The verification token sent to the user's email.
     * @return A response entity with the result of the verification.
     */
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

    /**
     * Sends a password reset link to the user's email address.
     *
     * @param email The email address of the user requesting the password reset.
     * @return A response entity with the result of the request.
     */
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

    /**
     * Resets the user's password using the provided reset token.
     *
     * @param requestBody Contains the reset token and the new password.
     * @return A response entity with the result of the password reset.
     */
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
}