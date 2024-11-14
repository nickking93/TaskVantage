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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

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
        System.out.println("JwtFilter is being executed for request: " + requestPath);

        // Skip JWT validation for public endpoints or static resources
        if (EXCLUDED_PATHS.contains(requestPath)) {
            System.out.println("Skipping JWT validation for path: " + requestPath);
            chain.doFilter(request, response); // Skip JWT validation for excluded paths
            return;
        }

        // Retrieve the Authorization header
        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("Authorization Header: " + authorizationHeader);

        String username = null;
        String jwt = null;

        // Check if the Authorization header contains a valid JWT token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Extract the token
            try {
                username = jwtUtil.getUsernameFromToken(jwt);  // Extract username from token
                System.out.println("Username extracted from token: " + username);
                System.out.println("Token expiration: " + jwtUtil.getExpirationDateFromToken(jwt));
            } catch (Exception e) {
                System.out.println("Error while extracting data from token: " + e.getMessage());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Invalid JWT token");
                return;
            }
        } else {
            // Allow access to public resources if no token is provided (e.g., static files)
            if (authorizationHeader == null) {
                System.out.println("Authorization header missing, but the path may be public.");
                chain.doFilter(request, response); // Proceed without authentication
                return;
            }

            // For non-public paths, respond with an unauthorized error
            System.out.println("Authorization header missing or does not start with Bearer ");
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
                System.out.println("JWT token validated and user authenticated: " + username);
            } else {
                System.out.println("JWT token validation failed for user: " + username);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Invalid JWT token");
                return;
            }
        }

        // Proceed with the next filter in the chain
        chain.doFilter(request, response);
    }
}