package com.taskvantage.backend.config;

import com.taskvantage.backend.Security.JwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Base64;

@TestConfiguration
public class TestSecurityConfig {

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Primary
    public String taskvantageFrontend() {
        return "http://localhost:4200";
    }

    @Bean
    @Primary
    public String googleCalendarClientId() {
        return "test-client-id";
    }

    @Bean
    @Primary
    public String googleCalendarClientSecret() {
        return "test-client-secret";
    }

    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return new JwtUtil() {
            @Override
            protected String getEnvVariable(String name) {
                if ("JWT_SECRET".equals(name)) {
                    return Base64.getEncoder().encodeToString(
                            "test-jwt-secret-key-for-testing-purposes-only".getBytes()
                    );
                }
                return super.getEnvVariable(name);
            }
        };
    }
}
