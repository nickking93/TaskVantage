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
        // Fetch the JWT_SECRET from the system environment variables
        String secret = System.getenv("JWT_SECRET");

        if (secret == null) {
            throw new IllegalArgumentException("JWT_SECRET environment variable is not set.");
        }

        // Decode the Base64 encoded key if it's stored that way
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        this.SECRET_KEY = Keys.hmacShaKeyFor(decodedKey);
    }

    // Extract username from the JWT token
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // New method to extract the username from the JWT token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract expiration date from the JWT token
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract a single claim from the JWT token
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return getExpirationDateFromToken(token).before(new Date());
    }

    // Validate the token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

        // Log validation results
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

    // Generate a token for a user
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String token = createToken(claims, userDetails.getUsername());
        // Log the generated token
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