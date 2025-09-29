package com.tariffsheriff.backend.security.redis;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.LocalDateTime;

/**
 * Redis entity for storing blacklisted JWT tokens.
 * Uses Redis TTL to automatically clean up expired tokens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("blacklisted_tokens")
public class BlacklistedToken {

    /**
     * The token ID (jti claim) - used as Redis key.
     */
    @Id
    private String tokenId;

    /**
     * Time to live in seconds - Redis will automatically delete after this time.
     * Should match the remaining time of the JWT token.
     */
    @TimeToLive
    private Long expiration;

    /**
     * Reason for blacklisting the token.
     */
    private String reason;

    /**
     * Timestamp when the token was blacklisted.
     */
    private LocalDateTime blacklistedAt;

    /**
     * Optional user ID for tracking purposes.
     */
    private Long userId;

    /**
     * Optional token type (access, refresh, custom).
     */
    private String tokenType;

    /**
     * Constructor for basic blacklisting.
     *
     * @param tokenId The token ID to blacklist
     * @param expiration TTL in seconds
     * @param reason Reason for blacklisting
     */
    public BlacklistedToken(String tokenId, Long expiration, String reason) {
        this.tokenId = tokenId;
        this.expiration = expiration;
        this.reason = reason;
        this.blacklistedAt = LocalDateTime.now();
    }

    /**
     * Constructor with user tracking.
     *
     * @param tokenId The token ID to blacklist
     * @param expiration TTL in seconds
     * @param reason Reason for blacklisting
     * @param userId User ID associated with the token
     * @param tokenType Type of token being blacklisted
     */
    public BlacklistedToken(String tokenId, Long expiration, String reason, Long userId, String tokenType) {
        this.tokenId = tokenId;
        this.expiration = expiration;
        this.reason = reason;
        this.userId = userId;
        this.tokenType = tokenType;
        this.blacklistedAt = LocalDateTime.now();
    }
}