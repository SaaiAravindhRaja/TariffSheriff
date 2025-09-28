package com.tariffsheriff.backend.security.jwt;

import com.tariffsheriff.backend.config.JwtConfig;
import com.tariffsheriff.backend.security.redis.BlacklistedToken;
import com.tariffsheriff.backend.security.redis.BlacklistedTokenRepository;
import com.tariffsheriff.backend.security.redis.BlacklistService;
import com.tariffsheriff.backend.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * High-level token service for JWT operations.
 * Handles token generation, validation, refresh, and blacklisting with Redis integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final BlacklistService blacklistService;

    /**
     * Generate both access and refresh tokens for a user.
     *
     * @param user The user for whom to generate tokens
     * @return TokenPair containing both access and refresh tokens
     */
    public TokenPair generateTokenPair(User user) {
        log.debug("Generating token pair for user: {}", user.getEmail());
        
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        
        log.debug("Generated token pair for user: {} with access token expiring in {} ms", 
                user.getEmail(), jwtConfig.getAccessTokenExpiration());
        
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * Generate a new access token for a user.
     *
     * @param user The user for whom to generate the token
     * @return JWT access token string
     */
    public String generateAccessToken(User user) {
        log.debug("Generating access token for user: {}", user.getEmail());
        return jwtUtil.generateAccessToken(user);
    }

    /**
     * Generate a new refresh token for a user.
     *
     * @param user The user for whom to generate the token
     * @return JWT refresh token string
     */
    public String generateRefreshToken(User user) {
        log.debug("Generating refresh token for user: {}", user.getEmail());
        return jwtUtil.generateRefreshToken(user);
    }

    /**
     * Validate an access token.
     *
     * @param token The token to validate
     * @param user The user to validate against
     * @return true if token is valid and not blacklisted, false otherwise
     */
    public boolean validateAccessToken(String token, User user) {
        try {
            // Check if token is of correct type
            if (!jwtUtil.isTokenOfType(token, JwtUtil.ACCESS_TOKEN_TYPE)) {
                log.warn("Token is not an access token for user: {}", user.getEmail());
                return false;
            }

            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                log.warn("Access token is blacklisted for user: {}", user.getEmail());
                return false;
            }

            // Validate token structure and claims
            boolean isValid = jwtUtil.validateToken(token, user);
            log.debug("Access token validation result for user {}: {}", user.getEmail(), isValid);
            
            return isValid;
        } catch (Exception e) {
            log.error("Error validating access token for user {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Validate a refresh token.
     *
     * @param token The token to validate
     * @param user The user to validate against
     * @return true if token is valid and not blacklisted, false otherwise
     */
    public boolean validateRefreshToken(String token, User user) {
        try {
            // Check if token is of correct type
            if (!jwtUtil.isTokenOfType(token, JwtUtil.REFRESH_TOKEN_TYPE)) {
                log.warn("Token is not a refresh token for user: {}", user.getEmail());
                return false;
            }

            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                log.warn("Refresh token is blacklisted for user: {}", user.getEmail());
                return false;
            }

            // Validate token structure and claims
            boolean isValid = jwtUtil.validateToken(token, user);
            log.debug("Refresh token validation result for user {}: {}", user.getEmail(), isValid);
            
            return isValid;
        } catch (Exception e) {
            log.error("Error validating refresh token for user {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Refresh tokens using a valid refresh token.
     * Blacklists the old refresh token and generates new token pair.
     *
     * @param refreshToken The refresh token to use for generating new tokens
     * @param user The user for whom to refresh tokens
     * @return New TokenPair with fresh access and refresh tokens
     * @throws JwtException if refresh token is invalid or blacklisted
     */
    public TokenPair refreshTokens(String refreshToken, User user) {
        log.debug("Refreshing tokens for user: {}", user.getEmail());

        // Validate the refresh token
        if (!validateRefreshToken(refreshToken, user)) {
            log.warn("Invalid refresh token provided for user: {}", user.getEmail());
            throw new JwtException("Invalid refresh token");
        }

        // Blacklist the old refresh token
        blacklistToken(refreshToken, "Token refresh");

        // Generate new token pair
        TokenPair newTokenPair = generateTokenPair(user);
        log.info("Successfully refreshed tokens for user: {}", user.getEmail());
        
        return newTokenPair;
    }

    /**
     * Blacklist a token with a reason.
     *
     * @param token The token to blacklist
     * @param reason The reason for blacklisting
     */
    public void blacklistToken(String token, String reason) {
        try {
            String tokenId = jwtUtil.extractTokenId(token);
            long remainingTime = jwtUtil.getRemainingTimeSeconds(token);
            
            if (remainingTime > 0) {
                Long userId = jwtUtil.extractUserId(token);
                String tokenType = jwtUtil.extractTokenType(token);
                
                blacklistService.blacklistToken(tokenId, remainingTime, reason, userId, tokenType);
                log.info("Token blacklisted with ID: {} for reason: {}", tokenId, reason);
            } else {
                log.debug("Token already expired, not adding to blacklist: {}", tokenId);
            }
        } catch (Exception e) {
            log.error("Error blacklisting token: {}", e.getMessage());
            // Don't throw exception here to avoid breaking logout flow
        }
    }

    /**
     * Check if a token is blacklisted.
     *
     * @param token The token to check
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String tokenId = jwtUtil.extractTokenId(token);
            boolean isBlacklisted = blacklistService.isTokenBlacklisted(tokenId);
            
            if (isBlacklisted) {
                log.debug("Token is blacklisted: {}", tokenId);
            }
            
            return isBlacklisted;
        } catch (Exception e) {
            log.error("Error checking token blacklist status: {}", e.getMessage());
            // If we can't check blacklist, assume token is valid to avoid blocking legitimate users
            return false;
        }
    }

    /**
     * Blacklist all tokens for a user (useful for logout from all devices).
     * Note: This is a simplified implementation. In a production system,
     * you might want to track user tokens more explicitly.
     *
     * @param user The user whose tokens should be blacklisted
     * @param reason The reason for blacklisting
     */
    public void blacklistAllUserTokens(User user, String reason) {
        log.info("Blacklisting all tokens for user: {} for reason: {}", user.getEmail(), reason);
        // This is a placeholder implementation
        // In a real system, you might maintain a user-token mapping
        // For now, we rely on individual token blacklisting
    }

    /**
     * Extract user information from a token without full validation.
     * Useful for getting user info from expired but structurally valid tokens.
     *
     * @param token The token to extract info from
     * @return TokenInfo containing user details from the token
     * @throws JwtException if token structure is invalid
     */
    public TokenInfo extractTokenInfo(String token) {
        try {
            if (!jwtUtil.validateTokenStructure(token)) {
                throw new JwtException("Invalid token structure");
            }

            String username = jwtUtil.extractUsername(token);
            Long userId = jwtUtil.extractUserId(token);
            String roles = jwtUtil.extractRoles(token);
            String tokenType = jwtUtil.extractTokenType(token);
            String tokenId = jwtUtil.extractTokenId(token);
            boolean isExpired = jwtUtil.isTokenExpired(token);
            long remainingTime = jwtUtil.getRemainingTimeSeconds(token);

            return TokenInfo.builder()
                    .username(username)
                    .userId(userId)
                    .roles(roles)
                    .tokenType(tokenType)
                    .tokenId(tokenId)
                    .expired(isExpired)
                    .remainingTimeSeconds(remainingTime)
                    .build();
        } catch (Exception e) {
            log.error("Error extracting token info: {}", e.getMessage());
            throw new JwtException("Failed to extract token information", e);
        }
    }

    /**
     * Check if a token needs refresh (expires within a certain threshold).
     *
     * @param token The token to check
     * @param thresholdMinutes Minutes before expiration to consider refresh needed
     * @return true if token should be refreshed, false otherwise
     */
    public boolean shouldRefreshToken(String token, int thresholdMinutes) {
        try {
            long remainingTimeSeconds = jwtUtil.getRemainingTimeSeconds(token);
            long thresholdSeconds = thresholdMinutes * 60L;
            
            boolean shouldRefresh = remainingTimeSeconds <= thresholdSeconds;
            log.debug("Token refresh check: remaining={}s, threshold={}s, shouldRefresh={}", 
                    remainingTimeSeconds, thresholdSeconds, shouldRefresh);
            
            return shouldRefresh;
        } catch (Exception e) {
            log.error("Error checking if token needs refresh: {}", e.getMessage());
            return true; // If we can't determine, assume refresh is needed
        }
    }

    /**
     * Create a custom token for specific purposes (password reset, email verification, etc.).
     *
     * @param user The user for whom to create the token
     * @param purpose The purpose of the token
     * @param expirationMinutes Expiration time in minutes
     * @return Custom JWT token string
     */
    public String createCustomToken(User user, String purpose, int expirationMinutes) {
        log.debug("Creating custom token for user: {} with purpose: {}", user.getEmail(), purpose);
        return jwtUtil.createCustomToken(user, purpose, expirationMinutes);
    }

    /**
     * Validate a custom token for a specific purpose.
     *
     * @param token The token to validate
     * @param user The user to validate against
     * @param expectedPurpose The expected purpose of the token
     * @return true if token is valid for the purpose, false otherwise
     */
    public boolean validateCustomToken(String token, User user, String expectedPurpose) {
        try {
            // Check if token is blacklisted
            if (isTokenBlacklisted(token)) {
                log.warn("Custom token is blacklisted for user: {}", user.getEmail());
                return false;
            }

            // Validate basic token structure and user
            if (!jwtUtil.validateToken(token, user)) {
                return false;
            }

            // Check if token is of custom type
            if (!jwtUtil.isTokenOfType(token, "custom")) {
                log.warn("Token is not a custom token for user: {}", user.getEmail());
                return false;
            }

            // Check if purpose matches
            String tokenPurpose = jwtUtil.extractPurpose(token);
            boolean purposeMatches = expectedPurpose.equals(tokenPurpose);
            
            log.debug("Custom token validation for user {}: purpose match = {}", 
                    user.getEmail(), purposeMatches);
            
            return purposeMatches;
        } catch (Exception e) {
            log.error("Error validating custom token for user {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * Data class for token pair (access + refresh tokens).
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}