package com.tariffsheriff.backend.security.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing token blacklist operations.
 * Provides high-level operations for blacklist management and automatic cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    /**
     * Blacklist a token with comprehensive tracking.
     *
     * @param tokenId The token ID to blacklist
     * @param expiration TTL in seconds
     * @param reason Reason for blacklisting
     * @param userId User ID associated with the token (optional)
     * @param tokenType Type of token (optional)
     */
    public void blacklistToken(String tokenId, Long expiration, String reason, Long userId, String tokenType) {
        try {
            BlacklistedToken blacklistedToken = new BlacklistedToken(
                    tokenId, expiration, reason, userId, tokenType
            );
            
            blacklistedTokenRepository.save(blacklistedToken);
            log.info("Token blacklisted: id={}, reason={}, userId={}, type={}", 
                    tokenId, reason, userId, tokenType);
        } catch (Exception e) {
            log.error("Failed to blacklist token {}: {}", tokenId, e.getMessage());
            throw new RuntimeException("Failed to blacklist token", e);
        }
    }

    /**
     * Blacklist a token with basic information.
     *
     * @param tokenId The token ID to blacklist
     * @param expiration TTL in seconds
     * @param reason Reason for blacklisting
     */
    public void blacklistToken(String tokenId, Long expiration, String reason) {
        blacklistToken(tokenId, expiration, reason, null, null);
    }

    /**
     * Check if a token is blacklisted.
     *
     * @param tokenId The token ID to check
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String tokenId) {
        try {
            return blacklistedTokenRepository.isTokenBlacklisted(tokenId);
        } catch (Exception e) {
            log.error("Error checking blacklist status for token {}: {}", tokenId, e.getMessage());
            // Return false to avoid blocking legitimate users if Redis is down
            return false;
        }
    }

    /**
     * Get blacklisted token details.
     *
     * @param tokenId The token ID to retrieve
     * @return BlacklistedToken if found, null otherwise
     */
    public BlacklistedToken getBlacklistedToken(String tokenId) {
        try {
            return blacklistedTokenRepository.getBlacklistedToken(tokenId).orElse(null);
        } catch (Exception e) {
            log.error("Error retrieving blacklisted token {}: {}", tokenId, e.getMessage());
            return null;
        }
    }

    /**
     * Get all blacklisted tokens for a user.
     *
     * @param userId The user ID to search for
     * @return List of blacklisted tokens for the user
     */
    public List<BlacklistedToken> getUserBlacklistedTokens(Long userId) {
        try {
            return blacklistedTokenRepository.findByUserId(userId);
        } catch (Exception e) {
            log.error("Error retrieving blacklisted tokens for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get count of blacklisted tokens for a user.
     *
     * @param userId The user ID to count tokens for
     * @return Number of blacklisted tokens for the user
     */
    public long getUserBlacklistedTokenCount(Long userId) {
        try {
            return blacklistedTokenRepository.countByUserId(userId);
        } catch (Exception e) {
            log.error("Error counting blacklisted tokens for user {}: {}", userId, e.getMessage());
            return 0;
        }
    }

    /**
     * Get blacklisted tokens by type.
     *
     * @param tokenType The token type to search for
     * @return List of blacklisted tokens of the specified type
     */
    public List<BlacklistedToken> getBlacklistedTokensByType(String tokenType) {
        try {
            return blacklistedTokenRepository.findByTokenType(tokenType);
        } catch (Exception e) {
            log.error("Error retrieving blacklisted tokens by type {}: {}", tokenType, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get blacklisted tokens by reason.
     *
     * @param reason The reason to search for
     * @return List of blacklisted tokens for the specified reason
     */
    public List<BlacklistedToken> getBlacklistedTokensByReason(String reason) {
        try {
            return blacklistedTokenRepository.findByReason(reason);
        } catch (Exception e) {
            log.error("Error retrieving blacklisted tokens by reason {}: {}", reason, e.getMessage());
            return List.of();
        }
    }

    /**
     * Clean up blacklisted tokens for a user.
     * This is mainly for administrative purposes as tokens should expire naturally.
     *
     * @param userId The user ID whose tokens should be cleaned up
     */
    public void cleanupUserTokens(Long userId) {
        try {
            long count = blacklistedTokenRepository.countByUserId(userId);
            blacklistedTokenRepository.deleteByUserId(userId);
            log.info("Cleaned up {} blacklisted tokens for user {}", count, userId);
        } catch (Exception e) {
            log.error("Error cleaning up tokens for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Get statistics about blacklisted tokens.
     *
     * @return BlacklistStats containing various statistics
     */
    public BlacklistStats getBlacklistStats() {
        try {
            long totalTokens = blacklistedTokenRepository.count();
            long accessTokens = blacklistedTokenRepository.countByTokenType("access");
            long refreshTokens = blacklistedTokenRepository.countByTokenType("refresh");
            long customTokens = blacklistedTokenRepository.countByTokenType("custom");

            return BlacklistStats.builder()
                    .totalBlacklistedTokens(totalTokens)
                    .accessTokens(accessTokens)
                    .refreshTokens(refreshTokens)
                    .customTokens(customTokens)
                    .build();
        } catch (Exception e) {
            log.error("Error retrieving blacklist statistics: {}", e.getMessage());
            return BlacklistStats.builder().build();
        }
    }

    /**
     * Scheduled task to log blacklist statistics.
     * Runs every hour to provide insights into token blacklist usage.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void logBlacklistStats() {
        try {
            BlacklistStats stats = getBlacklistStats();
            log.info("Blacklist Statistics - Total: {}, Access: {}, Refresh: {}, Custom: {}",
                    stats.getTotalBlacklistedTokens(),
                    stats.getAccessTokens(),
                    stats.getRefreshTokens(),
                    stats.getCustomTokens());
        } catch (Exception e) {
            log.error("Error logging blacklist statistics: {}", e.getMessage());
        }
    }

    /**
     * Statistics about blacklisted tokens.
     */
    @lombok.Builder
    @lombok.Data
    public static class BlacklistStats {
        private long totalBlacklistedTokens;
        private long accessTokens;
        private long refreshTokens;
        private long customTokens;
    }
}