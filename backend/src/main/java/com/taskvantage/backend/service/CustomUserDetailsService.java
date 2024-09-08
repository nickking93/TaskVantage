package com.taskvantage.backend.service;

import com.taskvantage.backend.model.User;
import com.taskvantage.backend.model.AuthRequest;
import com.taskvantage.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${TASKVANTAGE_FRONTEND}")  // Inject frontend URL from application properties
    private String frontendUrl;

    @Autowired
    private EmailService emailService; // Email service to send confirmation emails

    public CustomUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found with username: {}", username);
            throw new UsernameNotFoundException("User not found");
        }
        return new CustomUserDetails(user); // Return the custom UserDetails
    }

    public User findUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found with username: {}", username);
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    // New method to find a user by their verification token
    public User findUserByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid verification token"));
    }

    // New method to verify the user's email
    public boolean verifyUserEmail(String token) {
        User user = findUserByVerificationToken(token);
        if (user != null && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setVerificationToken(null); // Clear the token after verification
            saveUser(user);  // Use the saveUser method
            logger.info("Email verified for user: {}", user.getUsername());
            return true;
        } else {
            logger.warn("Email already verified or invalid token for user: {}", user != null ? user.getUsername() : "unknown");
            return false;
        }
    }

    // New saveUser method to save a user entity
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public String registerUser(AuthRequest authRequest) {
        if (userRepository.findByUsername(authRequest.getUsername()) != null) {
            logger.warn("Attempted to register with an already taken username: {}", authRequest.getUsername());
            return "Username is already taken.";
        }

        // Create new user and encode the password
        User user = new User();
        user.setUsername(authRequest.getUsername());
        user.setPassword(passwordEncoder.encode(authRequest.getPassword()));

        // Generate a verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setEmailVerified(false); // Set emailVerified to false initially

        // Set the frontend URL where the user will verify their email
        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        // Customize the email content
        String emailContent = "<html><body>"
                + "<h2>Email Verification</h2>"
                + "<p>Please verify your email by clicking the link below:</p>"
                + "<p><a href=\"" + verificationLink + "\" style=\"color: #1a73e8; text-decoration: none;\">Verify your email</a></p>"
                + "<br>"
                + "<p>If the above link doesn't work, you can copy and paste this URL into your browser:</p>"
                + "<p><a href=\"" + verificationLink + "\" style=\"color: #1a73e8; text-decoration: none;\">" + verificationLink + "</a></p>"
                + "</body></html>";

        try {
            // Attempt to send the verification email with HTML format
            emailService.sendEmail(user.getUsername(), "Email Verification", emailContent, true);  // true for HTML

            // Save the user only if the email was successfully sent
            saveUser(user);  // Use the saveUser method

            logger.info("User registered successfully with username: {}", authRequest.getUsername());
            return "Registration successful. Please check your email to verify your account.";

        } catch (Exception e) {
            // Handle email sending failure
            logger.error("Failed to send verification email to user: {}", authRequest.getUsername(), e);
            return "Failed to send verification email. Please try again.";
        }
    }

    public void updateUserToken(String username, String token) {
        logger.info("Received request to update FCM token for user: {}", username);

        if (token == null || token.isEmpty()) {
            logger.warn("Received an invalid FCM token for user: {}", username);
            return; // Avoid saving an empty or null token
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found with username: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        logger.info("Updating FCM token for user: {} with token: {}", username, token);

        user.setToken(token); // Set the new FCM token
        userRepository.save(user); // Save the updated user entity

        logger.info("FCM Token updated successfully for user: {}", username);
    }

    public void clearUserToken(String username) {
        logger.info("Received request to clear FCM token for user: {}", username);

        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found with username: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        logger.info("Clearing FCM token for user: {}", username);
        user.setToken(null); // Set the token to null
        userRepository.save(user); // Save the updated user entity

        logger.info("FCM Token cleared successfully for user: {}", username);
    }
}