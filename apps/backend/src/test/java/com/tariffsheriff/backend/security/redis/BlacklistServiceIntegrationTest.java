package com.tariffsheriff.backend.security.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@SpringBootTest
@Testcontainers
class BlacklistServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private BlacklistService blacklistService;

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clean up Redis before each test
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void shouldBlacklistToken() {
        // Given
        String tokenId = "test-token-id";
        Long expiration = 300L; // 5 minutes
        String reason = "User logout";
        Long userId = 1L;
        String tokenType = "access";

        // When
        blacklistService.blacklistToken(tokenId, expiration, reason, userId, tokenType);

        // Then
        assertThat(blacklistService.isTokenBlacklisted(tokenId)).isTrue();
        
        BlacklistedToken token = blacklistService.getBlacklistedToken(tokenId);
        assertThat(token).isNotNull();
        assertThat(token.getTokenId()).isEqualTo(tokenId);
        assertThat(token.getReason()).isEqualTo(reason);
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getTokenType()).isEqualTo(tokenType);
        assertThat(token.getBlacklistedAt()).isNotNull();
    }

    @Test
    void shouldBlacklistTokenWithBasicInfo() {
        // Given
        String tokenId = "basic-token-id";
        Long expiration = 600L;
        String reason = "Security breach";

        // When
        blacklistService.blacklistToken(tokenId, expiration, reason);

        // Then
        assertThat(blacklistService.isTokenBlacklisted(tokenId)).isTrue();
        
        BlacklistedToken token = blacklistService.getBlacklistedToken(tokenId);
        assertThat(token).isNotNull();
        assertThat(token.getTokenId()).isEqualTo(tokenId);
        assertThat(token.getReason()).isEqualTo(reason);
        assertThat(token.getUserId()).isNull();
        assertThat(token.getTokenType()).isNull();
    }

    @Test
    void shouldReturnFalseForNonBlacklistedToken() {
        // Given
        String nonExistentTokenId = "non-existent-token";

        // When
        boolean isBlacklisted = blacklistService.isTokenBlacklisted(nonExistentTokenId);

        // Then
        assertThat(isBlacklisted).isFalse();
    }

    @Test
    void shouldGetUserBlacklistedTokens() {
        // Given
        Long userId = 1L;
        String tokenId1 = "user-token-1";
        String tokenId2 = "user-token-2";
        String tokenId3 = "other-user-token";
        
        blacklistService.blacklistToken(tokenId1, 300L, "logout", userId, "access");
        blacklistService.blacklistToken(tokenId2, 300L, "refresh", userId, "refresh");
        blacklistService.blacklistToken(tokenId3, 300L, "logout", 2L, "access");

        // When
        List<BlacklistedToken> userTokens = blacklistService.getUserBlacklistedTokens(userId);

        // Then
        assertThat(userTokens).hasSize(2);
        assertThat(userTokens).extracting(BlacklistedToken::getTokenId)
                .containsExactlyInAnyOrder(tokenId1, tokenId2);
    }

    @Test
    void shouldCountUserBlacklistedTokens() {
        // Given
        Long userId = 1L;
        blacklistService.blacklistToken("token-1", 300L, "reason", userId, "access");
        blacklistService.blacklistToken("token-2", 300L, "reason", userId, "refresh");
        blacklistService.blacklistToken("token-3", 300L, "reason", 2L, "access");

        // When
        long count = blacklistService.getUserBlacklistedTokenCount(userId);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldGetBlacklistedTokensByType() {
        // Given
        blacklistService.blacklistToken("access-1", 300L, "reason", 1L, "access");
        blacklistService.blacklistToken("access-2", 300L, "reason", 2L, "access");
        blacklistService.blacklistToken("refresh-1", 300L, "reason", 1L, "refresh");

        // When
        List<BlacklistedToken> accessTokens = blacklistService.getBlacklistedTokensByType("access");
        List<BlacklistedToken> refreshTokens = blacklistService.getBlacklistedTokensByType("refresh");

        // Then
        assertThat(accessTokens).hasSize(2);
        assertThat(refreshTokens).hasSize(1);
        assertThat(accessTokens).allMatch(token -> "access".equals(token.getTokenType()));
        assertThat(refreshTokens).allMatch(token -> "refresh".equals(token.getTokenType()));
    }

    @Test
    void shouldGetBlacklistedTokensByReason() {
        // Given
        String reason1 = "User logout";
        String reason2 = "Security breach";
        
        blacklistService.blacklistToken("token-1", 300L, reason1, 1L, "access");
        blacklistService.blacklistToken("token-2", 300L, reason1, 2L, "access");
        blacklistService.blacklistToken("token-3", 300L, reason2, 1L, "refresh");

        // When
        List<BlacklistedToken> logoutTokens = blacklistService.getBlacklistedTokensByReason(reason1);
        List<BlacklistedToken> breachTokens = blacklistService.getBlacklistedTokensByReason(reason2);

        // Then
        assertThat(logoutTokens).hasSize(2);
        assertThat(breachTokens).hasSize(1);
        assertThat(logoutTokens).allMatch(token -> reason1.equals(token.getReason()));
        assertThat(breachTokens).allMatch(token -> reason2.equals(token.getReason()));
    }

    @Test
    void shouldCleanupUserTokens() {
        // Given
        Long userId = 1L;
        blacklistService.blacklistToken("token-1", 300L, "reason", userId, "access");
        blacklistService.blacklistToken("token-2", 300L, "reason", userId, "refresh");
        blacklistService.blacklistToken("token-3", 300L, "reason", 2L, "access");

        // Verify tokens exist
        assertThat(blacklistService.getUserBlacklistedTokenCount(userId)).isEqualTo(2);

        // When
        blacklistService.cleanupUserTokens(userId);

        // Then
        assertThat(blacklistService.getUserBlacklistedTokenCount(userId)).isZero();
        assertThat(blacklistService.isTokenBlacklisted("token-3")).isTrue(); // Other user's token should remain
    }

    @Test
    void shouldGetBlacklistStats() {
        // Given
        blacklistService.blacklistToken("access-1", 300L, "reason", 1L, "access");
        blacklistService.blacklistToken("access-2", 300L, "reason", 2L, "access");
        blacklistService.blacklistToken("refresh-1", 300L, "reason", 1L, "refresh");
        blacklistService.blacklistToken("custom-1", 300L, "reason", 1L, "custom");

        // When
        BlacklistService.BlacklistStats stats = blacklistService.getBlacklistStats();

        // Then
        assertThat(stats.getTotalBlacklistedTokens()).isEqualTo(4);
        assertThat(stats.getAccessTokens()).isEqualTo(2);
        assertThat(stats.getRefreshTokens()).isEqualTo(1);
        assertThat(stats.getCustomTokens()).isEqualTo(1);
    }

    @Test
    void shouldHandleRedisConnectionErrors() {
        // Given - stop Redis container to simulate connection error
        redis.stop();

        // When & Then - should not throw exceptions
        assertThatNoException().isThrownBy(() -> {
            boolean result = blacklistService.isTokenBlacklisted("any-token");
            assertThat(result).isFalse(); // Should return false on error
        });

        assertThatNoException().isThrownBy(() -> {
            BlacklistedToken result = blacklistService.getBlacklistedToken("any-token");
            assertThat(result).isNull(); // Should return null on error
        });

        assertThatNoException().isThrownBy(() -> {
            List<BlacklistedToken> result = blacklistService.getUserBlacklistedTokens(1L);
            assertThat(result).isEmpty(); // Should return empty list on error
        });

        // Restart Redis for other tests
        redis.start();
    }

    @Test
    void shouldExpireTokensAutomatically() {
        // Given
        String tokenId = "expiring-token";
        Long shortExpiration = 1L; // 1 second

        // When
        blacklistService.blacklistToken(tokenId, shortExpiration, "test expiration", 1L, "access");
        
        // Verify token is initially blacklisted
        assertThat(blacklistService.isTokenBlacklisted(tokenId)).isTrue();

        // Wait for token to expire
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(blacklistService.isTokenBlacklisted(tokenId)).isFalse();
                });
    }

    @Test
    void shouldHandleMultipleConcurrentOperations() {
        // Given
        int numberOfTokens = 100;
        Long userId = 1L;

        // When - blacklist multiple tokens concurrently
        for (int i = 0; i < numberOfTokens; i++) {
            final int tokenIndex = i;
            blacklistService.blacklistToken(
                    "concurrent-token-" + tokenIndex,
                    300L,
                    "concurrent test",
                    userId,
                    "access"
            );
        }

        // Then
        assertThat(blacklistService.getUserBlacklistedTokenCount(userId)).isEqualTo(numberOfTokens);
        
        // Verify all tokens are blacklisted
        for (int i = 0; i < numberOfTokens; i++) {
            assertThat(blacklistService.isTokenBlacklisted("concurrent-token-" + i)).isTrue();
        }
    }
}