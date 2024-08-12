package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.AuthRequest;
import com.taskvantage.backend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public String login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );
            return "Login successful for user: " + authRequest.getUsername();
        } catch (AuthenticationException e) {
            return "Login failed: " + e.getMessage();
        }
    }

    @PostMapping("/register")
    public String register(@RequestBody AuthRequest authRequest) {
        // Implement registration logic here (e.g., save user to database)
        return "Registration successful for user: " + authRequest.getUsername();
    }
}
