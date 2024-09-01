package com.taskvantage.backend.service;

import com.taskvantage.backend.model.User;
import com.taskvantage.backend.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateUserToken() {
        // Prepare test data
        String username = "testuser";
        String token = "sampleToken";
        User user = new User();
        user.setUsername(username);

        // Define the behavior of the mocked UserRepository
        Mockito.when(userRepository.findByUsername(username)).thenReturn(user);

        // Call the method to test
        customUserDetailsService.updateUserToken(username, token);

        // Verify the behavior
        Assertions.assertEquals(token, user.getToken());
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
    }

    @Test
    void testUpdateUserToken_UserNotFound() {
        // Prepare test data
        String username = "nonexistentuser";
        String token = "sampleToken";

        // Define the behavior of the mocked UserRepository
        Mockito.when(userRepository.findByUsername(username)).thenReturn(null);

        // Call the method to test and expect an exception
        Assertions.assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.updateUserToken(username, token);
        });
    }
}
