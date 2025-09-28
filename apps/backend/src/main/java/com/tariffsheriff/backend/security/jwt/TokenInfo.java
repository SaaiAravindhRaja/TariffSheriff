package com.tariffsheriff.backend.security.jwt;

import lombok.Builder;
import lombok.Data;

/**
 * Data class containing information extracted from a JWT token.
 * Used for token analysis and debugging without full validation.
 */
@Data
@Builder
public class TokenInfo {
    private String username;
    private Long userId;
    private String roles;
    private String tokenType;
    private String tokenId;
    private boolean expired;
    private long remainingTimeSeconds;
}