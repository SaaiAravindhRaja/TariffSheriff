package com.tariffsheriff.backend.security.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    private BlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        blacklistService = new BlacklistService(blacklistedTokenRepository);
    }

    @Test
    void shouldBlacklistTokenWithFullInfo() {
        // Given
        String tokenId = "test-token";
        Long expiration = 300L;
        String reason = "User logout";
        Long userId = 1L;
        String tokenType = "access";

        // When
        blacklistService.blacklistToken(tokenId, expiration, reason, userId, tokenType);

        // Then
        verify(blacklistedTokenRepository).save(argThat(token -> 
            token.getTokenId().equals(tokenId) &&
            token.getExpiration().equals(expiration) &&
            token.getReason().equals(reason) &&
            token.getUserId().equals(userId) &&
            token.getTokenType().equals(tokenType) &&
            token.getBlacklistedAt() != null
        ));
    }

    @Test
    void shouldBlacklistTokenWithBasicInfo() {
        // Given
        String tokenId = "basic-token";
        Long expiration = 600L;
        String reason = "Security breach";

        // When
        blacklistService.blacklistToken(tokenId, expiration, reason);

        // Then
        verify(blacklistedTokenRepository).save(argThat(token -> 
            token.getTokenId().equals(tokenId) &&
            token.getExpiration().equals(expiration) &&
            token.getReason().equals(reason) &&
            token.getUserId() == null &&
            token.getTokenType() == null
        ));
    }

    @Test
    void shouldHandleBlacklistingError() {
        // Given
        String tokenId = "error-token";
        when(blacklistedTokenRepository.save(any())).thenThrow(new RuntimeException("Redis error"));

        // When & Then
        assertThatThrownBy(() -> blacklistService.blacklistToken(tokenId, 300L, "test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to blacklist token");
    }

    @Test
    void shouldCheckIfTokenIsBlacklisted() {
        // Given
        String tokenId = "test-token";
        when(blacklistedTokenRepository.isTokenBlacklisted(tokenId)).thenReturn(true);

        // When
        boolean isBlacklisted = blacklistService.isTokenBlacklisted(tokenId);

        // Then
        assertThat(isBlacklisted).isTrue();
        verify(blacklistedTokenRepository).isTokenBlacklisted(tokenId);
    }

    @Test
    void shouldReturnFalseOnBlacklistCheckError() {
        // Given
        String tokenId = "error-token";
        when(blacklistedTokenRepository.isTokenBlacklisted(tokenId))
                .thenThrow(new RuntimeException("Redis connection error"));

        // When
        boolean isBlacklisted = blacklistService.isTokenBlacklisted(tokenId);

        // Then
        assertThat(isBlacklisted).isFalse(); // Should return false to avoid blocking users
    }

    @Test
    void shouldGetBlacklistedToken() {
        // Given
        String tokenId = "test-token";
        BlacklistedToken expectedToken = new BlacklistedToken(tokenId, 300L, "test reason");
        when(blacklistedTokenRepository.getBlacklistedToken(tokenId))
                .thenReturn(Optional.of(expectedToken));

        // When
        BlacklistedToken result = blacklistService.getBlacklistedToken(tokenId);

        // Then
        assertThat(result).isEqualTo(expectedToken);
        verify(blacklistedTokenRepository).getBlacklistedToken(tokenId);
    }

    @Test
    void shouldReturnNullWhenTokenNotFound() {
        // Given
        String tokenId = "non-existent-token";
        when(blacklistedTokenRepository.getBlacklistedToken(tokenId))
                .thenReturn(Optional.empty());

        // When
        BlacklistedToken result = blacklistService.getBlacklistedToken(tokenId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullOnGetTokenError() {
        // Given
        String tokenId = "error-token";
        when(blacklistedTokenRepository.getBlacklistedToken(tokenId))
                .thenThrow(new RuntimeException("Redis error"));

        // When
        BlacklistedToken result = blacklistService.getBlacklistedToken(tokenId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldGetUserBlacklistedTokens() {
        // Given
        Long userId = 1L;
        List<BlacklistedToken> expectedTokens = List.of(
                new BlacklistedToken("token1", 300L, "reason1"),
                new BlacklistedToken("token2", 300L, "reason2")
        );
        when(blacklistedTokenRepository.findByUserId(userId)).thenReturn(expectedTokens);

        // When
        List<BlacklistedToken> result = blacklistService.getUserBlacklistedTokens(userId);

        // Then
        assertThat(result).isEqualTo(expectedTokens);
        verify(blacklistedTokenRepository).findByUserId(userId);
    }

    @Test
    void shouldReturnEmptyListOnUserTokensError() {
        // Given
        Long userId = 1L;
        when(blacklistedTokenRepository.findByUserId(userId))
                .thenThrow(new RuntimeException("Redis error"));

        // When
        List<BlacklistedToken> result = blacklistService.getUserBlacklistedTokens(userId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCountUserBlacklistedTokens() {
        // Given
        Long userId = 1L;
        when(blacklistedTokenRepository.countByUserId(userId)).thenReturn(5L);

        // When
        long count = blacklistService.getUserBlacklistedTokenCount(userId);

        // Then
        assertThat(count).isEqualTo(5L);
        verify(blacklistedTokenRepository).countByUserId(userId);
    }

    @Test
    void shouldReturnZeroOnCountError() {
        // Given
        Long userId = 1L;
        when(blacklistedTokenRepository.countByUserId(userId))
                .thenThrow(new RuntimeException("Redis error"));

        // When
        long count = blacklistService.getUserBlacklistedTokenCount(userId);

        // Then
        assertThat(count).isZero();
    }

    @Test
    void shouldGetBlacklistedTokensByType() {
        // Given
        String tokenType = "access";
        List<BlacklistedToken> expectedTokens = List.of(
                new BlacklistedToken("token1", 300L, "reason1", 1L, tokenType),
                new BlacklistedToken("token2", 300L, "reason2", 2L, tokenType)
        );
        when(blacklistedTokenRepository.findByTokenType(tokenType)).thenReturn(expectedTokens);

        // When
        List<BlacklistedToken> result = blacklistService.getBlacklistedTokensByType(tokenType);

        // Then
        assertThat(result).isEqualTo(expectedTokens);
        verify(blacklistedTokenRepository).findByTokenType(tokenType);
    }

    @Test
    void shouldGetBlacklistedTokensByReason() {
        // Given
        String reason = "User logout";
        List<BlacklistedToken> expectedTokens = List.of(
                new BlacklistedToken("token1", 300L, reason),
                new BlacklistedToken("token2", 300L, reason)
        );
        when(blacklistedTokenRepository.findByReason(reason)).thenReturn(expectedTokens);

        // When
        List<BlacklistedToken> result = blacklistService.getBlacklistedTokensByReason(reason);

        // Then
        assertThat(result).isEqualTo(expectedTokens);
        verify(blacklistedTokenRepository).findByReason(reason);
    }

    @Test
    void shouldCleanupUserTokens() {
        // Given
        Long userId = 1L;
        when(blacklistedTokenRepository.countByUserId(userId)).thenReturn(3L);

        // When
        blacklistService.cleanupUserTokens(userId);

        // Then
        verify(blacklistedTokenRepository).countByUserId(userId);
        verify(blacklistedTokenRepository).deleteByUserId(userId);
    }

    @Test
    void shouldHandleCleanupError() {
        // Given
        Long userId = 1L;
        when(blacklistedTokenRepository.countByUserId(userId)).thenReturn(3L);
        doThrow(new RuntimeException("Redis error")).when(blacklistedTokenRepository).deleteByUserId(userId);

        // When & Then - should not throw exception
        assertThatNoException().isThrownBy(() -> blacklistService.cleanupUserTokens(userId));
    }

    @Test
    void shouldGetBlacklistStats() {
        // Given
        when(blacklistedTokenRepository.count()).thenReturn(10L);
        when(blacklistedTokenRepository.countByTokenType("access")).thenReturn(6L);
        when(blacklistedTokenRepository.countByTokenType("refresh")).thenReturn(3L);
        when(blacklistedTokenRepository.countByTokenType("custom")).thenReturn(1L);

        // When
        BlacklistService.BlacklistStats stats = blacklistService.getBlacklistStats();

        // Then
        assertThat(stats.getTotalBlacklistedTokens()).isEqualTo(10L);
        assertThat(stats.getAccessTokens()).isEqualTo(6L);
        assertThat(stats.getRefreshTokens()).isEqualTo(3L);
        assertThat(stats.getCustomTokens()).isEqualTo(1L);
    }

    @Test
    void shouldReturnEmptyStatsOnError() {
        // Given
        when(blacklistedTokenRepository.count()).thenThrow(new RuntimeException("Redis error"));

        // When
        BlacklistService.BlacklistStats stats = blacklistService.getBlacklistStats();

        // Then
        assertThat(stats.getTotalBlacklistedTokens()).isZero();
        assertThat(stats.getAccessTokens()).isZero();
        assertThat(stats.getRefreshTokens()).isZero();
        assertThat(stats.getCustomTokens()).isZero();
    }

    @Test
    void shouldLogBlacklistStats() {
        // Given
        when(blacklistedTokenRepository.count()).thenReturn(5L);
        when(blacklistedTokenRepository.countByTokenType("access")).thenReturn(3L);
        when(blacklistedTokenRepository.countByTokenType("refresh")).thenReturn(2L);
        when(blacklistedTokenRepository.countByTokenType("custom")).thenReturn(0L);

        // When & Then - should not throw exception
        assertThatNoException().isThrownBy(() -> blacklistService.logBlacklistStats());
    }

    @Test
    void shouldHandleLogStatsError() {
        // Given
        when(blacklistedTokenRepository.count()).thenThrow(new RuntimeException("Redis error"));

        // When & Then - should not throw exception
        assertThatNoException().isThrownBy(() -> blacklistService.logBlacklistStats());
    }
}