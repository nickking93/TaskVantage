package com.taskvantage.backend.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final Key SECRET_KEY;

    public JwtUtil() {
        String secret = System.getenv("JWT_SECRET");

        if (secret == null) {
            throw new IllegalArgumentException("JWT_SECRET environment variable is not set.");
        }

        // Decode the Base64 encoded key
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        this.SECRET_KEY = Keys.hmacShaKeyFor(decodedKey);

        // Debugging: Log the key (for non-production environments only)
        System.out.println("Decoded JWT Secret Key: " + Base64.getEncoder().encodeToString(decodedKey));
    }

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long getUserIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        System.out.println("Extracted claims: " + claims);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.out.println("Failed to extract claims: " + e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token");
        }
    }

    private Boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

        if (!isValid) {
            System.out.println("Token validation failed for user: " + username);
            if (!username.equals(userDetails.getUsername())) {
                System.out.println("Username in token does not match. Expected: " + userDetails.getUsername() + ", Found: " + username);
            }
            if (isTokenExpired(token)) {
                System.out.println("Token is expired.");
            }
        } else {
            System.out.println("Token is valid for user: " + username);
        }

        return isValid;
    }

    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);  // Add userId to the claims
        String token = createToken(claims, userDetails.getUsername());
        System.out.println("Generated JWT Token: " + token);
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(SECRET_KEY)  // Use the secure key
                .compact();
    }
}