package com.tariffsheriff.backend.security.redis;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing blacklisted tokens in Redis.
 * Provides CRUD operations and custom queries for token blacklist management.
 */
@Repository
public interface BlacklistedTokenRepository extends CrudRepository<BlacklistedToken, String> {

    /**
     * Find all blacklisted tokens for a specific user.
     *
     * @param userId The user ID to search for
     * @return List of blacklisted tokens for the user
     */
    List<BlacklistedToken> findByUserId(Long userId);

    /**
     * Find all blacklisted tokens of a specific type.
     *
     * @param tokenType The token type to search for (access, refresh, custom)
     * @return List of blacklisted tokens of the specified type
     */
    List<BlacklistedToken> findByTokenType(String tokenType);

    /**
     * Find all blacklisted tokens for a specific user and token type.
     *
     * @param userId The user ID to search for
     * @param tokenType The token type to search for
     * @return List of blacklisted tokens matching both criteria
     */
    List<BlacklistedToken> findByUserIdAndTokenType(Long userId, String tokenType);

    /**
     * Find all tokens blacklisted after a specific date.
     *
     * @param blacklistedAfter The date after which to search
     * @return List of tokens blacklisted after the specified date
     */
    List<BlacklistedToken> findByBlacklistedAtAfter(LocalDateTime blacklistedAfter);

    /**
     * Find all tokens blacklisted for a specific reason.
     *
     * @param reason The reason to search for
     * @return List of tokens blacklisted for the specified reason
     */
    List<BlacklistedToken> findByReason(String reason);

    /**
     * Count the number of blacklisted tokens for a specific user.
     *
     * @param userId The user ID to count tokens for
     * @return Number of blacklisted tokens for the user
     */
    long countByUserId(Long userId);

    /**
     * Count the number of blacklisted tokens of a specific type.
     *
     * @param tokenType The token type to count
     * @return Number of blacklisted tokens of the specified type
     */
    long countByTokenType(String tokenType);

    /**
     * Delete all blacklisted tokens for a specific user.
     * Note: This is mainly for cleanup purposes as tokens should expire naturally.
     *
     * @param userId The user ID whose tokens should be deleted
     */
    void deleteByUserId(Long userId);

    /**
     * Check if a token exists in the blacklist.
     * This is equivalent to existsById but more explicit.
     *
     * @param tokenId The token ID to check
     * @return true if token is blacklisted, false otherwise
     */
    default boolean isTokenBlacklisted(String tokenId) {
        return existsById(tokenId);
    }

    /**
     * Get blacklisted token details by token ID.
     *
     * @param tokenId The token ID to retrieve
     * @return Optional containing the blacklisted token if found
     */
    default Optional<BlacklistedToken> getBlacklistedToken(String tokenId) {
        return findById(tokenId);
    }
}