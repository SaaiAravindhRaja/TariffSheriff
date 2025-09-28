package com.tariffsheriff.backend.security.jwt;

import com.tariffsheriff.backend.config.JwtConfig;
import com.tariffsheriff.backend.user.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT utility class for token generation, validation, and parsing.
 * Provides comprehensive JWT operations for both access and refresh tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final JwtConfig jwtConfig;

    // Token type constants
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    // Claim names
    public static final String USER_ID_CLAIM = "userId";
    public static final String ROLES_CLAIM = "roles";
    public static final String TOKEN_TYPE_CLAIM = "tokenType";
    public static final String TOKEN_ID_CLAIM = "jti";

    /**
     * Generate an access token for the given user.
     *
     * @param user The user for whom to generate the token
     * @return JWT access token string
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, user.getId());
        claims.put(ROLES_CLAIM, user.getRole().name());
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        
        return createToken(claims, user.getEmail(), jwtConfig.getAccessTokenExpiration());
    }

    /**
     * Generate a refresh token for the given user.
     *
     * @param user The user for whom to generate the token
     * @return JWT refresh token string
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, user.getId());
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        
        return createToken(claims, user.getEmail(), jwtConfig.getRefreshTokenExpiration());
    }

    /**
     * Create a JWT token with the specified claims, subject, and expiration.
     *
     * @param claims Additional claims to include in the token
     * @param subject The subject (typically username/email)
     * @param expirationTime Expiration time in milliseconds
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationTime);
        String tokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .claim(TOKEN_ID_CLAIM, tokenId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(jwtConfig.getSecretKey())
                .compact();
    }

    /**
     * Extract the username (email) from the token.
     *
     * @param token JWT token
     * @return Username/email from the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the user ID from the token.
     *
     * @param token JWT token
     * @return User ID from the token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get(USER_ID_CLAIM, Long.class));
    }

    /**
     * Extract the user roles from the token.
     *
     * @param token JWT token
     * @return User roles from the token
     */
    public String extractRoles(String token) {
        return extractClaim(token, claims -> claims.get(ROLES_CLAIM, String.class));
    }

    /**
     * Extract the token type from the token.
     *
     * @param token JWT token
     * @return Token type (access or refresh)
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));
    }

    /**
     * Extract the token ID from the token.
     *
     * @param token JWT token
     * @return Token ID (jti claim)
     */
    public String extractTokenId(String token) {
        return extractClaim(token, claims -> claims.get(TOKEN_ID_CLAIM, String.class));
    }

    /**
     * Extract the expiration date from the token.
     *
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract the issued at date from the token.
     *
     * @param token JWT token
     * @return Issued at date
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Extract a specific claim from the token.
     *
     * @param token JWT token
     * @param claimsResolver Function to extract the desired claim
     * @param <T> Type of the claim
     * @return The extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from the token.
     *
     * @param token JWT token
     * @return All claims from the token
     * @throws JwtException if token is invalid
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(jwtConfig.getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid JWT signature", e);
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new JwtException("Invalid JWT token", e);
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            throw new JwtException("JWT token is expired", e);
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
            throw new JwtException("JWT token is unsupported", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw new JwtException("JWT claims string is empty", e);
        }
    }

    /**
     * Check if the token is expired.
     *
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            // If we can't extract expiration, consider it expired
            return true;
        }
    }

    /**
     * Validate the token against the user details.
     *
     * @param token JWT token
     * @param user User details to validate against
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token, User user) {
        try {
            final String username = extractUsername(token);
            final Long userId = extractUserId(token);
            
            return username.equals(user.getEmail()) 
                    && userId.equals(user.getId())
                    && !isTokenExpired(token);
        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate the token signature and structure without checking expiration.
     *
     * @param token JWT token
     * @return true if token structure and signature are valid, false otherwise
     */
    public boolean validateTokenStructure(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Token is expired but structurally valid
            return true;
        } catch (JwtException e) {
            log.error("Token structure validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if the token is of the specified type.
     *
     * @param token JWT token
     * @param expectedType Expected token type (access or refresh)
     * @return true if token is of expected type, false otherwise
     */
    public boolean isTokenOfType(String token, String expectedType) {
        try {
            String tokenType = extractTokenType(token);
            return expectedType.equals(tokenType);
        } catch (JwtException e) {
            log.error("Failed to extract token type: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the remaining time until token expiration in milliseconds.
     *
     * @param token JWT token
     * @return Remaining time in milliseconds, or 0 if expired/invalid
     */
    public long getRemainingTimeMillis(String token) {
        try {
            Date expiration = extractExpiration(token);
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (JwtException e) {
            return 0;
        }
    }

    /**
     * Get the remaining time until token expiration in seconds.
     *
     * @param token JWT token
     * @return Remaining time in seconds, or 0 if expired/invalid
     */
    public long getRemainingTimeSeconds(String token) {
        return getRemainingTimeMillis(token) / 1000;
    }

    /**
     * Convert Date to LocalDateTime for easier handling.
     *
     * @param date Date to convert
     * @return LocalDateTime representation
     */
    public LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Convert LocalDateTime to Date for JWT operations.
     *
     * @param localDateTime LocalDateTime to convert
     * @return Date representation
     */
    public Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Create a token with custom expiration time.
     * Useful for password reset tokens or email verification tokens.
     *
     * @param user User for whom to create the token
     * @param purpose Purpose of the token (e.g., "password_reset", "email_verification")
     * @param expirationMinutes Expiration time in minutes
     * @return JWT token string
     */
    public String createCustomToken(User user, String purpose, int expirationMinutes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID_CLAIM, user.getId());
        claims.put("purpose", purpose);
        claims.put(TOKEN_TYPE_CLAIM, "custom");
        
        long expirationTime = expirationMinutes * 60 * 1000L; // Convert to milliseconds
        return createToken(claims, user.getEmail(), expirationTime);
    }

    /**
     * Extract the purpose from a custom token.
     *
     * @param token JWT token
     * @return Purpose of the token
     */
    public String extractPurpose(String token) {
        return extractClaim(token, claims -> claims.get("purpose", String.class));
    }
}