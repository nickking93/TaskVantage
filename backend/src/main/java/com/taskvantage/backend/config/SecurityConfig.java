package com.taskvantage.backend.config;

import com.taskvantage.backend.Security.JwtFilter;
import com.taskvantage.backend.Security.SimpleLoggingFilter;
import com.taskvantage.backend.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.servlet.http.HttpServletRequest;  // Updated import to jakarta

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final SimpleLoggingFilter simpleLoggingFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JwtFilter jwtFilter, SimpleLoggingFilter simpleLoggingFilter) {
        this.jwtFilter = jwtFilter;
        this.simpleLoggingFilter = simpleLoggingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("Configuring SecurityFilterChain");

        return http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration corsConfig = new CorsConfiguration();
                    // Uncomment these lines after debugging
                    // corsConfig.addAllowedOrigin("https://localhost:4200");
                    // corsConfig.addAllowedOrigin("https://taskvantage-frontend-cbaab3e2bxcpbyb8.eastus-01.azurewebsites.net");

                    // Temporary allow all origins for debugging
                    corsConfig.addAllowedOrigin("*");
                    corsConfig.addAllowedMethod("*");
                    corsConfig.addAllowedHeader("*");
                    corsConfig.setAllowCredentials(true);

                    // Log the CORS configuration and incoming request details
                    logRequestDetails(request);
                    System.out.println("CORS Configuration: " + corsConfig.toString());

                    return corsConfig;
                }))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/login", "/api/register").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(simpleLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        // Uncomment these lines after debugging
        // config.addAllowedOrigin("https://localhost:4200");
        // config.addAllowedOrigin("https://taskvantage-frontend-cbaab3e2bxcpbyb8.eastus-01.azurewebsites.net");

        // Temporary allow all origins for debugging
        config.addAllowedOrigin("*");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }

    private void logRequestDetails(HttpServletRequest request) {
        System.out.println("Incoming request method: " + request.getMethod());
        System.out.println("Incoming request URL: " + request.getRequestURL());
        System.out.println("Incoming request headers: ");
        request.getHeaderNames().asIterator().forEachRemaining(header -> {
            System.out.println(header + ": " + request.getHeader(header));
        });
    }
}
