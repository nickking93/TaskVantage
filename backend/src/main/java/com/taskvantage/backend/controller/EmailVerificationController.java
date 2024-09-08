package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.User;
import com.taskvantage.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Optional;

@RestController
public class EmailVerificationController {

    private final UserRepository userRepository;

    public EmailVerificationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        // Find the user by the verification token
        Optional<User> userOptional = userRepository.findByVerificationToken(token);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.isEmailVerified()) {
                return ResponseEntity.badRequest().body("Email already verified.");
            }

            // Verify the user's email and clear the token
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            userRepository.save(user);

            return ResponseEntity.ok("Email verified successfully.");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired verification token.");
        }
    }
}
