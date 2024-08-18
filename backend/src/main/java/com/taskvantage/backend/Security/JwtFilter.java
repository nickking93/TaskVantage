package com.taskvantage.backend.Security;

import com.taskvantage.backend.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        System.out.println("JwtFilter is being executed for request: " + request.getRequestURI());

        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authorizationHeader);

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("Extracted JWT: " + jwt);
            try {
                username = jwtUtil.getUsernameFromToken(jwt);
                System.out.println("Username extracted from token: " + username);
                System.out.println("Token expiration: " + jwtUtil.getExpirationDateFromToken(jwt));
            } catch (Exception e) {
                // Log the exception and return an error response
                System.out.println("Error while extracting username from token: " + e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
                return; // Stop the filter chain here
            }
        } else {
            System.out.println("Authorization header is missing or does not start with 'Bearer '");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("Authentication context is null, proceeding with user authentication");
            UserDetails userDetails = this.customUserDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                System.out.println("JWT token is valid, setting authentication context for user: " + username);
                var authenticationToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            } else {
                // If token validation fails, log and return unauthorized
                System.out.println("JWT token validation failed for user: " + username);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT token");
                return; // Stop the filter chain here
            }
        }

        // Proceed with the filter chain
        System.out.println("Proceeding with the filter chain");
        chain.doFilter(request, response);
    }
}
