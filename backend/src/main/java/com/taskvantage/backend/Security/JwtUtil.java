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
    private static final long ACCESS_TOKEN_VALIDITY = 10 * 60 * 60 * 1000; // 10 hours
    private static final long REFRESH_TOKEN_VALIDITY = 30 * 24 * 60 * 60 * 1000; // 30 days
    private static final long PWA_TOKEN_VALIDITY = 90 * 24 * 60 * 60 * 1000; // 90 days

    public JwtUtil() {
        String secret = System.getenv("JWT_SECRET");

        if (secret == null) {
            throw new IllegalArgumentException("JWT_SECRET environment variable is not set.");
        }

        byte[] decodedKey = Base64.getDecoder().decode(secret);
        this.SECRET_KEY = Keys.hmacShaKeyFor(decodedKey);
    }

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long getUserIdFromToken(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String getClientTypeFromToken(String token) {
        return extractClaim(token, claims -> claims.get("clientType", String.class));
    }

    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
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
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
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

    public Map<String, String> generateTokens(UserDetails userDetails, Long userId, boolean isPwa) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", generateAccessToken(userDetails, userId, isPwa));
        tokens.put("refreshToken", generateRefreshToken(userDetails, userId, isPwa));
        return tokens;
    }

    private String generateAccessToken(UserDetails userDetails, Long userId, boolean isPwa) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "ACCESS");
        claims.put("clientType", isPwa ? "PWA" : "WEB");

        long validity = isPwa ? PWA_TOKEN_VALIDITY : ACCESS_TOKEN_VALIDITY;
        return createToken(claims, userDetails.getUsername(), validity);
    }

    private String generateRefreshToken(UserDetails userDetails, Long userId, boolean isPwa) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "REFRESH");
        claims.put("clientType", isPwa ? "PWA" : "WEB");

        long validity = isPwa ? PWA_TOKEN_VALIDITY : REFRESH_TOKEN_VALIDITY;
        return createToken(claims, userDetails.getUsername(), validity);
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(extractClaim(token, claims -> claims.get("tokenType", String.class)));
    }

    private String createToken(Map<String, Object> claims, String subject, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(SECRET_KEY)
                .compact();
    }

    public boolean isPwaToken(String token) {
        return "PWA".equals(getClientTypeFromToken(token));
    }

    public String generateTokenFromRefreshToken(String refreshToken, UserDetails userDetails) {
        if (!isRefreshToken(refreshToken) || isTokenExpired(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        Long userId = getUserIdFromToken(refreshToken);
        boolean isPwa = isPwaToken(refreshToken);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "ACCESS");
        claims.put("clientType", isPwa ? "PWA" : "WEB");

        long validity = isPwa ? PWA_TOKEN_VALIDITY : ACCESS_TOKEN_VALIDITY;
        return createToken(claims, userDetails.getUsername(), validity);
    }
}