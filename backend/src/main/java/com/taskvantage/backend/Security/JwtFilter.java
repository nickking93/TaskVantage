package com.taskvantage.backend.Security;

import com.taskvantage.backend.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    // List of paths that should be excluded from JWT validation
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/login", "/api/register", "/api/verify-email", "/api/forgot-password",
            "/api/reset-password", "/favicon.ico", "/test/google-calendar", "/oauth2/callback/google",
            "/login/oauth2/code/google"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        logger.debug("JwtFilter processing request: {}", requestPath);

        // Skip JWT validation for public endpoints or static resources
        if (EXCLUDED_PATHS.contains(requestPath)) {
            logger.debug("Skipping JWT validation for excluded path: {}", requestPath);
            chain.doFilter(request, response); // Skip JWT validation for excluded paths
            return;
        }

        // Retrieve the Authorization header
        final String authorizationHeader = request.getHeader("Authorization");
        logger.debug("Authorization header present: {}", authorizationHeader != null);

        String username = null;
        String jwt = null;

        // Check if the Authorization header contains a valid JWT token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Extract the token
            try {
                username = jwtUtil.getUsernameFromToken(jwt);  // Extract username from token
                logger.debug("Token validated successfully");
            } catch (Exception e) {
                logger.debug("Error extracting data from token: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Invalid JWT token");
                return;
            }
        } else {
            // Allow access to public resources if no token is provided (e.g., static files)
            if (authorizationHeader == null) {
                logger.debug("No authorization header for path: {}", requestPath);
                chain.doFilter(request, response); // Proceed without authentication
                return;
            }

            // For non-public paths, respond with an unauthorized error
            logger.debug("Invalid authorization header format");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Missing Authorization header");
            return;
        }

        // Authenticate the user if a valid token is provided
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.customUserDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                var authenticationToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                logger.debug("User authenticated successfully");
            } else {
                logger.debug("JWT token validation failed");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Invalid JWT token");
                return;
            }
        }

        // Proceed with the next filter in the chain
        chain.doFilter(request, response);
    }
}