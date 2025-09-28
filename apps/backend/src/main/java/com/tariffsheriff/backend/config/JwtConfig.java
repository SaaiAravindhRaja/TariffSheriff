package com.tariffsheriff.backend.config;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT configuration properties and utilities.
 * Centralizes JWT-related configuration for the authentication system.
 */
@Configuration
@Getter
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Get the secret key for JWT signing and verification.
     * Ensures the key is properly formatted for HMAC-SHA algorithms.
     */
    public SecretKey getSecretKey() {
        // Ensure the secret is long enough for HS256 (minimum 256 bits / 32 bytes)
        String paddedSecret = secret;
        if (secret.length() < 32) {
            paddedSecret = String.format("%-32s", secret).replace(' ', '0');
        }
        return Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Get access token expiration in milliseconds.
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Get refresh token expiration in milliseconds.
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * Get access token expiration in seconds (for JWT exp claim).
     */
    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }

    /**
     * Get refresh token expiration in seconds (for JWT exp claim).
     */
    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpiration / 1000;
    }
}