package com.taskvantage.backend.Security;

import com.taskvantage.backend.model.User;
import com.taskvantage.backend.service.CustomUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

/**
 * Utility class for authorization checks across controllers.
 * Validates that the authenticated user matches the requested resource owner.
 */
@Component
public class AuthorizationUtil {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public AuthorizationUtil(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Validates the authorization header format.
     */
    public boolean isValidAuthorizationHeader(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.startsWith("Bearer ");
    }

    /**
     * Extracts the JWT token from the Authorization header.
     */
    public String extractToken(String authorizationHeader) {
        if (!isValidAuthorizationHeader(authorizationHeader)) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    /**
     * Extracts the user ID from the JWT token.
     * Returns null if the token is invalid.
     */
    public Long getUserIdFromToken(String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        if (token == null) {
            return null;
        }
        try {
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Validates that the user ID in the request matches the authenticated user.
     * Returns an error response if validation fails, or null if validation passes.
     */
    public ResponseEntity<Map<String, Object>> validateUserAccess(
            String authorizationHeader,
            Long requestedUserId) {

        if (!isValidAuthorizationHeader(authorizationHeader)) {
            return createErrorResponse("Unauthorized: Missing or invalid Authorization header", 401);
        }

        String token = extractToken(authorizationHeader);
        Long tokenUserId;

        try {
            tokenUserId = jwtUtil.getUserIdFromToken(token);
        } catch (ExpiredJwtException e) {
            return createErrorResponse("Unauthorized: Token expired", 401);
        } catch (SignatureException e) {
            return createErrorResponse("Unauthorized: Invalid token signature", 401);
        } catch (MalformedJwtException e) {
            return createErrorResponse("Unauthorized: Malformed token", 401);
        } catch (Exception e) {
            return createErrorResponse("Unauthorized: Invalid token", 401);
        }

        if (tokenUserId == null || !tokenUserId.equals(requestedUserId)) {
            return createErrorResponse("Forbidden: Access denied to this resource", 403);
        }

        return null; // Validation passed
    }

    /**
     * Validates that the authenticated user owns the specified resource.
     * Returns an error response if validation fails, or null if validation passes.
     */
    public ResponseEntity<Map<String, Object>> validateResourceOwnership(
            String authorizationHeader,
            Long resourceOwnerId) {

        if (!isValidAuthorizationHeader(authorizationHeader)) {
            return createErrorResponse("Unauthorized: Missing or invalid Authorization header", 401);
        }

        String token = extractToken(authorizationHeader);
        Long tokenUserId;

        try {
            tokenUserId = jwtUtil.getUserIdFromToken(token);
        } catch (ExpiredJwtException e) {
            return createErrorResponse("Unauthorized: Token expired", 401);
        } catch (SignatureException e) {
            return createErrorResponse("Unauthorized: Invalid token signature", 401);
        } catch (MalformedJwtException e) {
            return createErrorResponse("Unauthorized: Malformed token", 401);
        } catch (Exception e) {
            return createErrorResponse("Unauthorized: Invalid token", 401);
        }

        if (tokenUserId == null || !tokenUserId.equals(resourceOwnerId)) {
            return createErrorResponse("Forbidden: You do not have access to this resource", 403);
        }

        return null; // Validation passed
    }

    /**
     * Validates that the authenticated user has admin privileges.
     * Returns an error response if validation fails, or null if validation passes.
     */
    public ResponseEntity<Map<String, Object>> validateAdminAccess(String authorizationHeader) {
        if (!isValidAuthorizationHeader(authorizationHeader)) {
            return createErrorResponse("Unauthorized: Missing or invalid Authorization header", 401);
        }

        String token = extractToken(authorizationHeader);
        Long tokenUserId;

        try {
            tokenUserId = jwtUtil.getUserIdFromToken(token);
        } catch (ExpiredJwtException e) {
            return createErrorResponse("Unauthorized: Token expired", 401);
        } catch (SignatureException e) {
            return createErrorResponse("Unauthorized: Invalid token signature", 401);
        } catch (MalformedJwtException e) {
            return createErrorResponse("Unauthorized: Malformed token", 401);
        } catch (Exception e) {
            return createErrorResponse("Unauthorized: Invalid token", 401);
        }

        if (tokenUserId == null) {
            return createErrorResponse("Unauthorized: Invalid token", 401);
        }

        // Check if user has admin privileges
        User user = userDetailsService.findUserById(tokenUserId);
        if (user == null) {
            return createErrorResponse("Unauthorized: User not found", 401);
        }

        if (!user.isAdmin()) {
            return createErrorResponse("Forbidden: Admin privileges required", 403);
        }

        return null; // Validation passed
    }

    /**
     * Creates a standardized error response.
     */
    public ResponseEntity<Map<String, Object>> createErrorResponse(String message, int statusCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return ResponseEntity.status(statusCode).body(response);
    }
}
