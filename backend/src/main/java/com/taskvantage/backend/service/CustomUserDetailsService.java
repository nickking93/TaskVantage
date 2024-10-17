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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${TASKVANTAGE_FRONTEND}")
    private String frontendUrl;

    @Autowired
    private EmailService emailService;

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
        return new CustomUserDetails(user);
    }

    public User findUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logger.error("User not found with username: {}", username);
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    public User findUserByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid verification token"));
    }

    public boolean verifyUserEmail(String token) {
        User user = findUserByVerificationToken(token);
        if (user != null && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            saveUser(user);
            logger.info("Email verified for user: {}", user.getUsername());
            return true;
        } else {
            logger.warn("Email already verified or invalid token for user: {}", user != null ? user.getUsername() : "unknown");
            return false;
        }
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public String registerUser(AuthRequest authRequest) {
        if (userRepository.findByUsername(authRequest.getUsername()) != null) {
            logger.warn("Attempted to register with an already taken username: {}", authRequest.getUsername());
            return "Username is already taken.";
        }

        User user = new User();
        user.setUsername(authRequest.getUsername());
        user.setPassword(passwordEncoder.encode(authRequest.getPassword()));

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setEmailVerified(false);

        String verificationLink = frontendUrl + "/verify-email?token=" + verificationToken;

        String emailContent = "<html><body>"
                + "<h2>Email Verification</h2>"
                + "<p>Please verify your email by clicking the link below:</p>"
                + "<p><a href=\"" + verificationLink + "\" style=\"color: #1a73e8; text-decoration: none;\">Verify your email</a></p>"
                + "</body></html>";

        try {
            emailService.sendEmail(user.getUsername(), "Email Verification", emailContent, true);
            saveUser(user);
            logger.info("User registered successfully with username: {}", authRequest.getUsername());
            return "Registration successful. Please check your email to verify your account.";
        } catch (Exception e) {
            logger.error("Failed to send verification email to user: {}", authRequest.getUsername(), e);
            return "Failed to send verification email. Please try again.";
        }
    }

    public void updateUserToken(String username, String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Invalid FCM token for user: {}", username);
            return;
        }

        User user = findUserByUsername(username);
        user.setToken(token);
        saveUser(user);
        logger.info("FCM Token updated successfully for user: {}", username);
    }

    public void clearUserToken(String username) {
        User user = findUserByUsername(username);
        user.setToken(null);
        saveUser(user);
        logger.info("FCM Token cleared successfully for user: {}", username);
    }

    /**
     * Sends a password reset link to the user's email.
     *
     * @param email The user's email address.
     * @return True if the reset link was sent successfully, false otherwise.
     */
    public boolean sendPasswordResetLink(String email) {
        User user = userRepository.findByUsername(email);
        if (user == null) {
            logger.warn("User not found with email: {}", email);
            return false;
        }

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));  // Token valid for 1 hour
        saveUser(user);

        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        String emailContent = "<html><body>"
                + "<h2>Password Reset</h2>"
                + "<p>Please reset your password by clicking the link below:</p>"
                + "<p><a href=\"" + resetLink + "\" style=\"color: #1a73e8; text-decoration: none;\">Reset your password</a></p>"
                + "</body></html>";

        try {
            emailService.sendEmail(user.getUsername(), "Password Reset", emailContent, true);
            logger.info("Password reset email sent to user: {}", user.getUsername());
            return true;
        } catch (Exception e) {
            logger.error("Failed to send password reset email to user: {}", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Validates the password reset token and checks if it has expired.
     *
     * @param token The reset token.
     * @return The associated user if the token is valid and not expired.
     * @throws UsernameNotFoundException If the token is invalid or expired.
     */
    public User validatePasswordResetToken(String token) {
        Optional<User> optionalUser = userRepository.findByPasswordResetToken(token);
        User user = optionalUser.orElseThrow(() -> new UsernameNotFoundException("Invalid password reset token"));

        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Password reset token expired for user: {}", user.getUsername());
            throw new UsernameNotFoundException("Password reset token expired");
        }

        return user;
    }

    /**
     * Updates the user's password after verifying the reset token.
     *
     * @param token The password reset token.
     * @param newPassword The new password to be set.
     * @return True if the password was updated successfully, false otherwise.
     */
    public boolean updatePassword(String token, String newPassword) {
        User user = validatePasswordResetToken(token);

        // Encrypt and set the new password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);  // Invalidate the token after successful reset
        user.setPasswordResetTokenExpiry(null);
        saveUser(user);

        logger.info("Password updated successfully for user: {}", user.getUsername());
        return true;
    }

    public User findUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            return userOptional.get();
        } else {
            logger.error("User not found with id: {}", userId);
            throw new UsernameNotFoundException("User not found with id: " + userId);
        }
    }
}