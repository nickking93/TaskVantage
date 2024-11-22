package com.taskvantage.backend.repository;

import com.taskvantage.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository provides methods to interact with the User entity in the database.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their username.
     *
     * @param username The username of the user.
     * @return The User entity.
     */
    User findByUsername(String username);

    /**
     * Finds a user by their Google email.
     *
     * @param googleEmail The Google email of the user.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByGoogleEmail(String googleEmail);

    /**
     * Finds a user by their email verification token.
     *
     * @param verificationToken The token used for email verification.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByVerificationToken(String verificationToken);

    /**
     * Finds a user by their password reset token.
     *
     * @param passwordResetToken The token used for password reset.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByPasswordResetToken(String passwordResetToken);

    /**
     * Finds a user by their FCM token.
     *
     * @param token The FCM token used for push notifications.
     * @return An Optional containing the User if found.
     */
    Optional<User> findByToken(String token);

    /**
     * Finds all users who have valid FCM tokens.
     * This query filters out users with null or empty tokens and ensures they are email verified.
     *
     * @return List of users with valid FCM tokens
     */
    @Query("SELECT u FROM User u WHERE u.token IS NOT NULL AND u.token != '' AND u.emailVerified = true")
    List<User> findUsersWithValidTokens();

    /**
     * Finds all users who have specific FCM token.
     * This can be used to check for duplicate tokens across users.
     *
     * @param token The FCM token to search for
     * @return List of users with the specified token
     */
    @Query("SELECT u FROM User u WHERE u.token = :token")
    List<User> findAllByToken(String token);
}