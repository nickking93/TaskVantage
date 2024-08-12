package com.taskvantage.backend.config;

import com.taskvantage.backend.model.User;
import com.taskvantage.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if the test user already exists
            if (userRepository.findByUsername("testuser") == null) {
                User user = new User();
                user.setUsername("testuser");
                user.setPassword(passwordEncoder.encode("password"));
                userRepository.save(user);
                System.out.println("Test user created: username=testuser, password=password");
            } else {
                System.out.println("Test user already exists.");
            }
        };
    }
}
