package com.taskvantage.backend.repository;

import com.taskvantage.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}