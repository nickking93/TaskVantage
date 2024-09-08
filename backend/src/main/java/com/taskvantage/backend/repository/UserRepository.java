package com.taskvantage.backend.repository;

import com.taskvantage.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    // Add method to find user by verification token
    Optional<User> findByVerificationToken(String verificationToken);
}