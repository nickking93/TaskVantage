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

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * The filter intercepts HTTP requests to check for JWT authentication tokens.
     * If the token is valid, it sets the user authentication in the security context.
     *
     * @param request  The HTTP request object.
     * @param response The HTTP response object.
     * @param chain    The filter chain.
     * @throws ServletException In case of general servlet errors.
     * @throws IOException      In case of I/O errors.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        System.out.println("JwtFilter is being executed for request: " + request.getRequestURI());

        String requestPath = request.getRequestURI();

        // Skip JWT validation for public endpoints (login, registration, verify-email, forgot-password, reset-password)
        if (requestPath.equals("/api/login") || requestPath.equals("/api/register") ||
                requestPath.equals("/api/verify-email") || requestPath.equals("/api/forgot-password") ||
                requestPath.equals("/api/reset-password")) {
            chain.doFilter(request, response);
            return;
        }

        // Retrieve the Authorization header
        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("Authorization Header: " + authorizationHeader);

        String username = null;
        String jwt = null;
        Long userIdFromToken = null;

        // Check if the Authorization header contains a valid JWT token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7); // Extract the token
            try {
                username = jwtUtil.getUsernameFromToken(jwt);  // Extract username from token
                userIdFromToken = jwtUtil.getUserIdFromToken(jwt);  // Extract userId from token
                System.out.println("Username extracted from token: " + username);
                System.out.println("UserId extracted from token: " + userIdFromToken);
                System.out.println("Token expiration: " + jwtUtil.getExpirationDateFromToken(jwt));
            } catch (Exception e) {
                System.out.println("Error while extracting data from token: " + e.getMessage());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Invalid JWT token");
                return;
            }
        } else {
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