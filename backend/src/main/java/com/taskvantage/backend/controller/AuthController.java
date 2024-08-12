package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.AuthRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            // Create a response map to return as JSON
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("username", authRequest.getUsername());

            // If you have a token (e.g., JWT), include it in the response
            // String token = generateJwtToken(authentication);
            // response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            // Create a response map for the error
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.status(401).body(errorResponse);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest authRequest) {
        // Implement registration logic here (e.g., save user to database)
        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful for user: " + authRequest.getUsername());
        return ResponseEntity.ok(response);
    }
}
