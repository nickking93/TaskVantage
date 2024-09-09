package com.taskvantage.backend.config;

import com.taskvantage.backend.Security.JwtFilter;
import com.taskvantage.backend.Security.SimpleLoggingFilter;
import com.taskvantage.backend.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final SimpleLoggingFilter simpleLoggingFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtFilter jwtFilter, SimpleLoggingFilter simpleLoggingFilter) {
        this.jwtFilter = jwtFilter;
        this.simpleLoggingFilter = simpleLoggingFilter;
    }

    /**
     * Configures the security filter chain for HTTP requests.
     *
     * @param http The HttpSecurity object for configuring security.
     * @return A SecurityFilterChain for the application.
     * @throws Exception if there are configuration errors.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration corsConfig = new CorsConfiguration();
                    corsConfig.addAllowedOrigin("http://localhost:4200");
                    corsConfig.addAllowedOrigin("https://taskvantage-frontend-cbaab3e2bxcpbyb8.eastus-01.azurewebsites.net");
                    corsConfig.addAllowedMethod("GET");
                    corsConfig.addAllowedMethod("POST");
                    corsConfig.addAllowedMethod("PUT");
                    corsConfig.addAllowedMethod("PATCH");
                    corsConfig.addAllowedMethod("DELETE");
                    corsConfig.addAllowedHeader("*");
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .csrf(csrf -> csrf.disable())  // Disable CSRF for stateless JWT authentication
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints that do not require authentication
                        .requestMatchers("/api/login", "/api/register", "/api/verify-email", "/api/forgot-password", "/api/reset-password").permitAll() // Add reset-password to the allowed list
                        // All other endpoints require authentication
                        .requestMatchers("/api/tasks/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // Stateless session for JWT
                .addFilterBefore(simpleLoggingFilter, UsernamePasswordAuthenticationFilter.class)  // Logging filter before authentication
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)  // JWT filter for authentication
                .build();
    }

    /**
     * Configures the authentication manager to be used with the security configuration.
     *
     * @param authenticationConfiguration Provides the current authentication configuration.
     * @return An AuthenticationManager instance.
     * @throws Exception if there are configuration errors.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configures CORS settings for handling cross-origin requests from the frontend.
     *
     * @return A CorsFilter instance that applies the CORS configuration to all endpoints.
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedOrigin("https://taskvantage-frontend-cbaab3e2bxcpbyb8.eastus-01.azurewebsites.net");

        config.addAllowedMethod("*");  // Allow all HTTP methods (GET, POST, etc.)
        config.addAllowedHeader("*");  // Allow all headers
        config.setAllowCredentials(true);  // Allow credentials (cookies, headers)

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}