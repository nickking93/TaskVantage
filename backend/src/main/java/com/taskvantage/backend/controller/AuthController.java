package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.AuthRequest;
import com.taskvantage.backend.model.User;
import com.taskvantage.backend.service.CustomUserDetailsService;
import com.taskvantage.backend.Security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest authRequest) {
        System.out.println("Login request received for user: " + authRequest.getUsername()); // Log request received

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            // Retrieve user details from the database
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(authRequest.getUsername());

            // Generate JWT token
            String token = jwtUtil.generateToken(userDetails);
            System.out.println("Generated Token: " + token);  // Log the generated token

            // Retrieve the actual User entity using the username
            User user = customUserDetailsService.findUserByUsername(authRequest.getUsername());

            // Update the user's FCM token if provided
            if (authRequest.getFcmToken() != null && !authRequest.getFcmToken().isEmpty()) {
                customUserDetailsService.updateUserToken(user.getUsername(), authRequest.getFcmToken());
                System.out.println("Updated FCM Token for user: " + user.getUsername());
            }

            // Create a response map to return as JSON
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", user.getUsername());
            response.put("userId", user.getId()); // Retrieve the userId from the User entity
            response.put("token", token); // Include the JWT token in the response

            System.out.println("Login successful for user: " + user.getUsername()); // Log success

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            System.out.println("Login failed for user: " + authRequest.getUsername() + ". Error: " + e.getMessage()); // Log failure

            // Create a response map for the error
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }


    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest authRequest) {
        String result = customUserDetailsService.registerUser(authRequest);

        Map<String, Object> response = new HashMap<>();
        if (result.equals("User registered successfully.")) {
            response.put("message", result);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", result);
            return ResponseEntity.status(400).body(response);
        }
    }

    @PostMapping("/update-token")
    public ResponseEntity<Map<String, Object>> updateFcmToken(@RequestBody Map<String, String> tokenRequest) {
        String username = jwtUtil.extractUsername(tokenRequest.get("token")); // Extract username from the JWT token
        String fcmToken = tokenRequest.get("fcmToken");

        if (username != null && fcmToken != null && !fcmToken.isEmpty()) {
            customUserDetailsService.updateUserToken(username, fcmToken);
            System.out.println("Updated FCM Token for user: " + username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "FCM Token updated successfully");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Invalid token or FCM token");
            return ResponseEntity.status(400).body(errorResponse);
        }
    }
}
