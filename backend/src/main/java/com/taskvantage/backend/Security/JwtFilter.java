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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        System.out.println("JwtFilter is being executed for request: " + request.getRequestURI());

        String requestPath = request.getRequestURI();
        if (requestPath.equals("/api/login") || requestPath.equals("/api/register")) {
            chain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("Authorization Header: " + authorizationHeader);

        String username = null;
        String jwt = null;
        Long userIdFromToken = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwt);
                userIdFromToken = jwtUtil.getUserIdFromToken(jwt); // Extract userId from the token
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

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.customUserDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                var authenticationToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                System.out.println("JWT token validated and user authenticated: " + username);

                // Validate userId from token with the one in the request path for both /home/ and /tasks/ paths
                String[] pathParts = requestPath.split("/");
                if (pathParts.length > 2 && (pathParts[1].equals("home") || pathParts[1].equals("tasks"))) {
                    try {
                        Long userIdFromPath = Long.parseLong(pathParts[2]);
                        if (!userIdFromToken.equals(userIdFromPath)) {
                            System.out.println("User ID mismatch: Token userId = " + userIdFromToken + ", Path userId = " + userIdFromPath);
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: User ID mismatch");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid userId in the request path: " + pathParts[2]);
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request: Invalid User ID");
                        return;
                    }
                }

            } else {
                System.out.println("JWT token validation failed for user: " + username);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden: Invalid JWT token");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}