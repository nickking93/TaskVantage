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

/**
 * CustomUserDetailsService implements UserDetailsService to manage user-related operations.
 */
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

    /**
     * Load user details by username. It checks both username and Google email.
     *
     * @param username the username or Google email of the user
     * @return UserDetails object containing user information
     * @throws UsernameNotFoundException if the user is not found
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findUser(username);
        return new CustomUserDetails(user);
    }

    /**
     * Finds a user by their username or Google email.
     *
     * This method first attempts to find a user using the provided username.
     * If no user is found, it then checks if the username corresponds to a Google email.
     *
     * @param username the username or Google email of the user to be found
     * @return the User object if found
     * @throws UsernameNotFoundException if no user is found with the given username or Google email
     */
    private User findUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            // Check if the username is a Google email
            user = userRepository.findByGoogleEmail(username).orElse(null);
        }
        if (user == null) {
            logger.debug("User not found with username or Google email");
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    /**
     * Find a user by their username or Google email.
     *
     * @param username the username or Google email of the user
     * @return User object if found
     * @throws UsernameNotFoundException if the user is not found
     */
    public User findUserByUsername(String username) throws UsernameNotFoundException {
        return findUser(username);
    }

    /**
     * Find a user by their FCM token.
     *
     * @param token the FCM token to search for
     * @return Optional<User> containing the user if found
     */
    public Optional<User> findUserByFCMToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Attempted to find user with null or empty FCM token");
            return Optional.empty();
        }
        return userRepository.findByToken(token);
    }

    /**
     * Find a user by their Google email.
     *
     * @param googleEmail the Google email of the user
     * @return User object if found
     * @throws UsernameNotFoundException if the user is not found
     */
    public User findUserByGoogleEmail(String googleEmail) throws UsernameNotFoundException {
        User user = userRepository.findByGoogleEmail(googleEmail).orElse(null);
        if (user == null) {
            logger.debug("User not found with Google email");
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    /**
     * Find a user by their email verification token.
     *
     * @param token the verification token
     * @return User object if found
     * @throws UsernameNotFoundException if the token is invalid
     */
    public User findUserByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid verification token"));
    }

    /**
     * Verify a user's email using a verification token.
     *
     * @param token the verification token
     * @return true if the email is verified, false otherwise
     */
    public boolean verifyUserEmail(String token) {
        User user = findUserByVerificationToken(token);
        if (user != null && !user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setVerificationToken(null);
            saveUser(user);
            logger.info("Email verified for user ID: {}", user.getId());
            return true;
        } else {
            logger.debug("Email already verified or invalid token");
            return false;
        }
    }

    /**
     * Save the user to the database.
     *
     * @param user the User object to save
     * @return the saved User object
     */
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Register a new user.
     *
     * @param authRequest the AuthRequest containing registration details
     * @return message indicating success or failure of registration
     */
    public String registerUser(AuthRequest authRequest) {
        if (userRepository.findByUsername(authRequest.getUsername()) != null) {
            logger.debug("Registration attempted with already taken username");
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
            logger.info("User registered successfully");
            return "Registration successful. Please check your email to verify your account.";
        } catch (Exception e) {
            logger.error("Failed to send verification email", e);
            return "Failed to send verification email. Please try again.";
        }
    }

    /**
     * Update the FCM token for a user.
     *
     * @param username the username of the user
     * @param token    the FCM token to set
     */
    public void updateUserToken(String username, String token) {
        if (token == null || token.isEmpty()) {
            logger.debug("Invalid FCM token provided");
            return;
        }

        User user = findUserByUsername(username);
        user.setToken(token);
        saveUser(user);
        logger.debug("FCM Token updated successfully for user ID: {}", user.getId());
    }

    /**
     * Clear the FCM token for a user and save the update.
     *
     * @param username the username of the user
     */
    public void clearUserToken(String username) {
        try {
            User user = findUserByUsername(username);
            user.setToken(null);
            saveUser(user);
            logger.debug("FCM Token cleared successfully for user ID: {}", user.getId());
        } catch (UsernameNotFoundException e) {
            logger.debug("Failed to clear FCM token - user not found");
        } catch (Exception e) {
            logger.error("Unexpected error while clearing FCM token", e);
        }
    }

    /**
     * Sends a password reset link to the user's email.
     *
     * @param email the user's email address
     * @return true if the reset link was sent successfully, false otherwise
     */
    public boolean sendPasswordResetLink(String email) {
        User user = userRepository.findByUsername(email);
        if (user == null) {
            logger.debug("Password reset requested for non-existent user");
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
            logger.info("Password reset email sent for user ID: {}", user.getId());
            return true;
        } catch (Exception e) {
            logger.error("Failed to send password reset email", e);
            return false;
        }
    }

    /**
     * Validate the password reset token and checks if it has expired.
     *
     * @param token the reset token
     * @return the associated user if the token is valid and not expired
     * @throws UsernameNotFoundException if the token is invalid or expired
     */
    public User validatePasswordResetToken(String token) {
        Optional<User> optionalUser = userRepository.findByPasswordResetToken(token);
        User user = optionalUser.orElseThrow(() -> new UsernameNotFoundException("Invalid password reset token"));

        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.debug("Password reset token expired for user ID: {}", user.getId());
            throw new UsernameNotFoundException("Password reset token expired");
        }

        return user;
    }

    /**
     * Update the user's password after verifying the reset token.
     *
     * @param token       the password reset token
     * @param newPassword the new password to be set
     * @return true if the password was updated successfully, false otherwise
     */
    public boolean updatePassword(String token, String newPassword) {
        User user = validatePasswordResetToken(token);

        // Encrypt and set the new password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);  // Invalidate the token after successful reset
        user.setPasswordResetTokenExpiry(null);
        saveUser(user);

        logger.info("Password updated successfully for user ID: {}", user.getId());
        return true;
    }

    /**
     * Find a user by their ID.
     *
     * @param userId the ID of the user
     * @return the User object if found
     * @throws UsernameNotFoundException if the user is not found
     */
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