package com.tariffsheriff.backend.security.jwt;

import com.tariffsheriff.backend.config.JwtConfig;
import com.tariffsheriff.backend.security.redis.BlacklistedTokenRepository;
import com.tariffsheriff.backend.security.redis.BlacklistService;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Mock
    private BlacklistService blacklistService;

    private TokenService tokenService;
    private User testUser;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(jwtUtil, jwtConfig, blacklistedTokenRepository, blacklistService);

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());

        // Mock JWT config
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
    }

    @Test
    void shouldGenerateTokenPair() {
        // Given
        String expectedAccessToken = "access.token.here";
        String expectedRefreshToken = "refresh.token.here";
        
        when(jwtUtil.generateAccessToken(testUser)).thenReturn(expectedAccessToken);
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn(expectedRefreshToken);

        // When
        TokenService.TokenPair tokenPair = tokenService.generateTokenPair(testUser);

        // Then
        assertThat(tokenPair).isNotNull();
        assertThat(tokenPair.getAccessToken()).isEqualTo(expectedAccessToken);
        assertThat(tokenPair.getRefreshToken()).isEqualTo(expectedRefreshToken);
        
        verify(jwtUtil).generateAccessToken(testUser);
        verify(jwtUtil).generateRefreshToken(testUser);
    }

    @Test
    void shouldGenerateAccessToken() {
        // Given
        String expectedToken = "access.token.here";
        when(jwtUtil.generateAccessToken(testUser)).thenReturn(expectedToken);

        // When
        String token = tokenService.generateAccessToken(testUser);

        // Then
        assertThat(token).isEqualTo(expectedToken);
        verify(jwtUtil).generateAccessToken(testUser);
    }

    @Test
    void shouldGenerateRefreshToken() {
        // Given
        String expectedToken = "refresh.token.here";
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn(expectedToken);

        // When
        String token = tokenService.generateRefreshToken(testUser);

        // Then
        assertThat(token).isEqualTo(expectedToken);
        verify(jwtUtil).generateRefreshToken(testUser);
    }

    @Test
    void shouldValidateAccessToken() {
        // Given
        String token = "valid.access.token";
        
        when(jwtUtil.isTokenOfType(token, JwtUtil.ACCESS_TOKEN_TYPE)).thenReturn(true);
        when(blacklistService.isTokenBlacklisted(anyString())).thenReturn(false);
        when(jwtUtil.validateToken(token, testUser)).thenReturn(true);
        when(jwtUtil.extractTokenId(token)).thenReturn("token-id");

        // When
        boolean isValid = tokenService.validateAccessToken(token, testUser);

        // Then
        assertThat(isValid).isTrue();
        verify(jwtUtil).isTokenOfType(token, JwtUtil.ACCESS_TOKEN_TYPE);
        verify(blacklistService).isTokenBlacklisted("token-id");
        verify(jwtUtil).validateToken(token, testUser);
    }

    @Test
    void shouldRejectWrongTokenType() {
        // Given
        String token = "refresh.token.here";
        when(jwtUtil.isTokenOfType(token, JwtUtil.ACCESS_TOKEN_TYPE)).thenReturn(false);

        // When
        boolean isValid = tokenService.validateAccessToken(token, testUser);

        // Then
        assertThat(isValid).isFalse();
        verify(jwtUtil).isTokenOfType(token, JwtUtil.ACCESS_TOKEN_TYPE);
        verifyNoMoreInteractions(blacklistService, jwtUtil);
    }

    @Test
    void shouldRejectBlacklistedToken() {
        // Given
        String token = "blacklisted.token";
        
        when(jwtUtil.isTokenOfType(token, JwtUtil.ACCESS_TOKEN_TYPE)).thenReturn(true);
        when(jwtUtil.extractTokenId(token)).thenReturn("token-id");
        when(blacklistService.isTokenBlacklisted("token-id")).thenReturn(true);

        // When
        boolean isValid = tokenService.validateAccessToken(token, testUser);

        // Then
        assertThat(isValid).isFalse();
        verify(blacklistService).isTokenBlacklisted("token-id");
        verify(jwtUtil, never()).validateToken(any(), any());
    }

    @Test
    void shouldValidateRefreshToken() {
        // Given
        String token = "valid.refresh.token";
        
        when(jwtUtil.isTokenOfType(token, JwtUtil.REFRESH_TOKEN_TYPE)).thenReturn(true);
        when(blacklistService.isTokenBlacklisted(anyString())).thenReturn(false);
        when(jwtUtil.validateToken(token, testUser)).thenReturn(true);
        when(jwtUtil.extractTokenId(token)).thenReturn("token-id");

        // When
        boolean isValid = tokenService.validateRefreshToken(token, testUser);

        // Then
        assertThat(isValid).isTrue();
        verify(jwtUtil).isTokenOfType(token, JwtUtil.REFRESH_TOKEN_TYPE);
        verify(blacklistService).isTokenBlacklisted("token-id");
        verify(jwtUtil).validateToken(token, testUser);
    }

    @Test
    void shouldRefreshTokens() {
        // Given
        String oldRefreshToken = "old.refresh.token";
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";
        
        when(jwtUtil.isTokenOfType(oldRefreshToken, JwtUtil.REFRESH_TOKEN_TYPE)).thenReturn(true);
        when(blacklistService.isTokenBlacklisted(anyString())).thenReturn(false);
        when(jwtUtil.validateToken(oldRefreshToken, testUser)).thenReturn(true);
        when(jwtUtil.extractTokenId(oldRefreshToken)).thenReturn("old-token-id");
        when(jwtUtil.generateAccessToken(testUser)).thenReturn(newAccessToken);
        when(jwtUtil.generateRefreshToken(testUser)).thenReturn(newRefreshToken);

        // When
        TokenService.TokenPair newTokenPair = tokenService.refreshTokens(oldRefreshToken, testUser);

        // Then
        assertThat(newTokenPair).isNotNull();
        assertThat(newTokenPair.getAccessToken()).isEqualTo(newAccessToken);
        assertThat(newTokenPair.getRefreshToken()).isEqualTo(newRefreshToken);
        
        verify(jwtUtil).generateAccessToken(testUser);
        verify(jwtUtil).generateRefreshToken(testUser);
    }

    @Test
    void shouldRejectInvalidRefreshToken() {
        // Given
        String invalidRefreshToken = "invalid.refresh.token";
        
        when(jwtUtil.isTokenOfType(invalidRefreshToken, JwtUtil.REFRESH_TOKEN_TYPE)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> tokenService.refreshTokens(invalidRefreshToken, testUser))
                .isInstanceOf(JwtException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void shouldBlacklistToken() {
        // Given
        String token = "token.to.blacklist";
        String reason = "User logout";
        String tokenId = "token-id";
        Long userId = 1L;
        String tokenType = "access";
        
        when(jwtUtil.extractTokenId(token)).thenReturn(tokenId);
        when(jwtUtil.getRemainingTimeSeconds(token)).thenReturn(300L);
        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(jwtUtil.extractTokenType(token)).thenReturn(tokenType);

        // When
        tokenService.blacklistToken(token, reason);

        // Then
        verify(blacklistService).blacklistToken(tokenId, 300L, reason, userId, tokenType);
    }

    @Test
    void shouldNotBlacklistExpiredToken() {
        // Given
        String expiredToken = "expired.token";
        String reason = "User logout";
        
        when(jwtUtil.extractTokenId(expiredToken)).thenReturn("token-id");
        when(jwtUtil.getRemainingTimeSeconds(expiredToken)).thenReturn(0L);

        // When
        tokenService.blacklistToken(expiredToken, reason);

        // Then
        verify(blacklistService, never()).blacklistToken(any(), any(), any(), any(), any());
    }

    @Test
    void shouldCheckIfTokenIsBlacklisted() {
        // Given
        String token = "some.token";
        String tokenId = "token-id";
        
        when(jwtUtil.extractTokenId(token)).thenReturn(tokenId);
        when(blacklistService.isTokenBlacklisted(tokenId)).thenReturn(true);

        // When
        boolean isBlacklisted = tokenService.isTokenBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isTrue();
        verify(blacklistService).isTokenBlacklisted(tokenId);
    }

    @Test
    void shouldExtractTokenInfo() {
        // Given
        String token = "valid.token";
        
        when(jwtUtil.validateTokenStructure(token)).thenReturn(true);
        when(jwtUtil.extractUsername(token)).thenReturn("test@example.com");
        when(jwtUtil.extractUserId(token)).thenReturn(1L);
        when(jwtUtil.extractRoles(token)).thenReturn("USER");
        when(jwtUtil.extractTokenType(token)).thenReturn("access");
        when(jwtUtil.extractTokenId(token)).thenReturn("token-id");
        when(jwtUtil.isTokenExpired(token)).thenReturn(false);
        when(jwtUtil.getRemainingTimeSeconds(token)).thenReturn(300L);

        // When
        TokenInfo tokenInfo = tokenService.extractTokenInfo(token);

        // Then
        assertThat(tokenInfo).isNotNull();
        assertThat(tokenInfo.getUsername()).isEqualTo("test@example.com");
        assertThat(tokenInfo.getUserId()).isEqualTo(1L);
        assertThat(tokenInfo.getRoles()).isEqualTo("USER");
        assertThat(tokenInfo.getTokenType()).isEqualTo("access");
        assertThat(tokenInfo.getTokenId()).isEqualTo("token-id");
        assertThat(tokenInfo.isExpired()).isFalse();
        assertThat(tokenInfo.getRemainingTimeSeconds()).isEqualTo(300L);
    }

    @Test
    void shouldThrowExceptionForInvalidTokenStructure() {
        // Given
        String invalidToken = "invalid.token";
        when(jwtUtil.validateTokenStructure(invalidToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> tokenService.extractTokenInfo(invalidToken))
                .isInstanceOf(JwtException.class)
                .hasMessage("Invalid token structure");
    }

    @Test
    void shouldCheckIfTokenNeedsRefresh() {
        // Given
        String token = "token.near.expiry";
        int thresholdMinutes = 5;
        
        when(jwtUtil.getRemainingTimeSeconds(token)).thenReturn(240L); // 4 minutes

        // When
        boolean shouldRefresh = tokenService.shouldRefreshToken(token, thresholdMinutes);

        // Then
        assertThat(shouldRefresh).isTrue();
    }

    @Test
    void shouldNotRefreshTokenWithTimeRemaining() {
        // Given
        String token = "token.with.time";
        int thresholdMinutes = 5;
        
        when(jwtUtil.getRemainingTimeSeconds(token)).thenReturn(600L); // 10 minutes

        // When
        boolean shouldRefresh = tokenService.shouldRefreshToken(token, thresholdMinutes);

        // Then
        assertThat(shouldRefresh).isFalse();
    }

    @Test
    void shouldCreateCustomToken() {
        // Given
        String purpose = "password_reset";
        int expirationMinutes = 60;
        String expectedToken = "custom.token";
        
        when(jwtUtil.createCustomToken(testUser, purpose, expirationMinutes)).thenReturn(expectedToken);

        // When
        String token = tokenService.createCustomToken(testUser, purpose, expirationMinutes);

        // Then
        assertThat(token).isEqualTo(expectedToken);
        verify(jwtUtil).createCustomToken(testUser, purpose, expirationMinutes);
    }

    @Test
    void shouldValidateCustomToken() {
        // Given
        String token = "custom.token";
        String purpose = "email_verification";
        
        when(blacklistService.isTokenBlacklisted(anyString())).thenReturn(false);
        when(jwtUtil.validateToken(token, testUser)).thenReturn(true);
        when(jwtUtil.isTokenOfType(token, "custom")).thenReturn(true);
        when(jwtUtil.extractPurpose(token)).thenReturn(purpose);
        when(jwtUtil.extractTokenId(token)).thenReturn("token-id");

        // When
        boolean isValid = tokenService.validateCustomToken(token, testUser, purpose);

        // Then
        assertThat(isValid).isTrue();
        verify(jwtUtil).validateToken(token, testUser);
        verify(jwtUtil).isTokenOfType(token, "custom");
        verify(jwtUtil).extractPurpose(token);
    }

    @Test
    void shouldRejectCustomTokenWithWrongPurpose() {
        // Given
        String token = "custom.token";
        String expectedPurpose = "password_reset";
        String actualPurpose = "email_verification";
        
        when(blacklistService.isTokenBlacklisted(anyString())).thenReturn(false);
        when(jwtUtil.validateToken(token, testUser)).thenReturn(true);
        when(jwtUtil.isTokenOfType(token, "custom")).thenReturn(true);
        when(jwtUtil.extractPurpose(token)).thenReturn(actualPurpose);
        when(jwtUtil.extractTokenId(token)).thenReturn("token-id");

        // When
        boolean isValid = tokenService.validateCustomToken(token, testUser, expectedPurpose);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldHandleExceptionsGracefully() {
        // Given
        String token = "problematic.token";
        
        when(jwtUtil.extractTokenId(token)).thenThrow(new RuntimeException("Token parsing error"));

        // When
        boolean isBlacklisted = tokenService.isTokenBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isFalse(); // Should return false on error to avoid blocking users
    }
}