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

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class); // Corrected the logger name

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest authRequest) {
        System.out.println("Login request received for user: " + authRequest.getUsername());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            // Fetch user details and verify email
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(authRequest.getUsername());
            User user = customUserDetailsService.findUserByUsername(authRequest.getUsername());

            // Check if the user has verified their email
            if (!user.isEmailVerified()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Your email is not verified. Please verify your email before logging in.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);  // 401 for unverified email
            }

            // Generate JWT token if email is verified
            String token = jwtUtil.generateToken(userDetails, user.getId());
            System.out.println("Generated Token: " + token);

            if (authRequest.getFcmToken() != null && !authRequest.getFcmToken().isEmpty()) {
                customUserDetailsService.updateUserToken(user.getUsername(), authRequest.getFcmToken());
                System.out.println("Updated FCM Token for user: " + user.getUsername());
            }

            // Prepare successful login response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", user.getUsername());
            response.put("userId", user.getId());
            response.put("token", token);

            System.out.println("Login successful for user: " + user.getUsername());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            System.out.println("Login failed for user: " + authRequest.getUsername() + ". Error: " + e.getMessage());

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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);  // Return 400 if registration fails due to email issues
        }
    }

    @PostMapping("/users/{username}/clear-fcm-token")
    public ResponseEntity<?> clearFcmToken(@PathVariable("username") String username) {
        try {
            customUserDetailsService.clearUserToken(username); // Clear token using the username
            return ResponseEntity.ok("FCM Token cleared successfully.");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + username);
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("token") String token) {
        try {
            // Find the user by the verification token
            User user = customUserDetailsService.findUserByVerificationToken(token);

            if (user == null) {
                // Return error response if the token is invalid
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("message", "Invalid verification token.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // Set email_verified to true and clear the verification token
            user.setEmailVerified(true);
            user.setVerificationToken(null); // Remove the token after successful verification
            customUserDetailsService.saveUser(user); // Save the updated user

            // Return a success response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Email successfully verified.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Handle any unexpected errors
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "An error occurred while verifying the email.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}