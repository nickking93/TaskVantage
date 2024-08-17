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
        System.out.println("Register request received for user: " + authRequest.getUsername()); // Log request received

        String result = customUserDetailsService.registerUser(authRequest);

        Map<String, Object> response = new HashMap<>();
        if (result.equals("User registered successfully.")) {
            System.out.println("Registration successful for user: " + authRequest.getUsername()); // Log success
            response.put("message", result);
            return ResponseEntity.ok(response);
        } else {
            System.out.println("Registration failed for user: " + authRequest.getUsername() + ". Reason: " + result); // Log failure
            response.put("message", result);
            return ResponseEntity.status(400).body(response);
        }
    }
}
