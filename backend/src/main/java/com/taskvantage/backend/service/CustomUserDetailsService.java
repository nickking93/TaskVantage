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

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class); // Corrected the logger name

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    public String registerUser(AuthRequest authRequest) {
        if (userRepository.findByUsername(authRequest.getUsername()) != null) {
            logger.warn("Attempted to register with an already taken username: {}", authRequest.getUsername());
            return "Username is already taken.";
        }

        User user = new User();
        user.setUsername(authRequest.getUsername());
        user.setPassword(passwordEncoder.encode(authRequest.getPassword()));
        userRepository.save(user);

        logger.info("User registered successfully with username: {}", authRequest.getUsername());
        return "User registered successfully.";
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
}