package com.tariffsheriff.backend.security.jwt;

import com.tariffsheriff.backend.config.JwtConfig;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private JwtConfig jwtConfig;

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Mock JWT configuration
        lenient().when(jwtConfig.getSecret()).thenReturn("mySecretKeyForTestingPurposes123456789");
        lenient().when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L); // 15 minutes
        lenient().when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L); // 7 days
        lenient().when(jwtConfig.getSecretKey()).thenCallRealMethod();

        jwtUtil = new JwtUtil(jwtConfig);

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldGenerateAccessToken() {
        // When
        String token = jwtUtil.generateAccessToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts

        // Verify token content
        String username = jwtUtil.extractUsername(token);
        Long userId = jwtUtil.extractUserId(token);
        String roles = jwtUtil.extractRoles(token);
        String tokenType = jwtUtil.extractTokenType(token);

        assertThat(username).isEqualTo(testUser.getEmail());
        assertThat(userId).isEqualTo(testUser.getId());
        assertThat(roles).isEqualTo(testUser.getRole().name());
        assertThat(tokenType).isEqualTo(JwtUtil.ACCESS_TOKEN_TYPE);
    }

    @Test
    void shouldGenerateRefreshToken() {
        // When
        String token = jwtUtil.generateRefreshToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3);

        // Verify token content
        String username = jwtUtil.extractUsername(token);
        Long userId = jwtUtil.extractUserId(token);
        String tokenType = jwtUtil.extractTokenType(token);

        assertThat(username).isEqualTo(testUser.getEmail());
        assertThat(userId).isEqualTo(testUser.getId());
        assertThat(tokenType).isEqualTo(JwtUtil.REFRESH_TOKEN_TYPE);
    }

    @Test
    void shouldExtractAllClaimsFromToken() {
        // Given
        String token = jwtUtil.generateAccessToken(testUser);

        // When
        Claims claims = jwtUtil.extractAllClaims(token);

        // Then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(testUser.getEmail());
        assertThat(claims.get(JwtUtil.USER_ID_CLAIM, Long.class)).isEqualTo(testUser.getId());
        assertThat(claims.get(JwtUtil.ROLES_CLAIM, String.class)).isEqualTo(testUser.getRole().name());
        assertThat(claims.get(JwtUtil.TOKEN_TYPE_CLAIM, String.class)).isEqualTo(JwtUtil.ACCESS_TOKEN_TYPE);
        assertThat(claims.get(JwtUtil.TOKEN_ID_CLAIM, String.class)).isNotNull();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void shouldExtractSpecificClaims() {
        // Given
        String token = jwtUtil.generateAccessToken(testUser);

        // When & Then
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(testUser.getEmail());
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(testUser.getId());
        assertThat(jwtUtil.extractRoles(token)).isEqualTo(testUser.getRole().name());
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo(JwtUtil.ACCESS_TOKEN_TYPE);
        assertThat(jwtUtil.extractTokenId(token)).isNotNull();
        assertThat(jwtUtil.extractExpiration(token)).isNotNull();
        assertThat(jwtUtil.extractIssuedAt(token)).isNotNull();
    }

    @Test
    void shouldValidateValidToken() {
        // Given
        String token = jwtUtil.generateAccessToken(testUser);

        // When
        boolean isValid = jwtUtil.validateToken(token, testUser);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        // Given
        String token = jwtUtil.generateAccessToken(testUser);
        
        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setEmail("different@example.com");

        // When
        boolean isValid = jwtUtil.validateToken(token, differentUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldDetectExpiredToken() {
        // Given - create token with very short expiration
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(1L); // 1 millisecond
        String token = jwtUtil.generateAccessToken(testUser);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Then
        assertThat(isExpired).isTrue();
    }

    @Test
    void shouldValidateTokenStructure() {
        // Given
        String validToken = jwtUtil.generateAccessToken(testUser);
        String invalidToken = "invalid.token.structure";

        // When & Then
        assertThat(jwtUtil.validateTokenStructure(validToken)).isTrue();
        assertThat(jwtUtil.validateTokenStructure(invalidToken)).isFalse();
    }

    @Test
    void shouldCheckTokenType() {
        // Given
        String accessToken = jwtUtil.generateAccessToken(testUser);
        String refreshToken = jwtUtil.generateRefreshToken(testUser);

        // When & Then
        assertThat(jwtUtil.isTokenOfType(accessToken, JwtUtil.ACCESS_TOKEN_TYPE)).isTrue();
        assertThat(jwtUtil.isTokenOfType(accessToken, JwtUtil.REFRESH_TOKEN_TYPE)).isFalse();
        
        assertThat(jwtUtil.isTokenOfType(refreshToken, JwtUtil.REFRESH_TOKEN_TYPE)).isTrue();
        assertThat(jwtUtil.isTokenOfType(refreshToken, JwtUtil.ACCESS_TOKEN_TYPE)).isFalse();
    }

    @Test
    void shouldCalculateRemainingTime() {
        // Given
        String token = jwtUtil.generateAccessToken(testUser);

        // When
        long remainingTimeMillis = jwtUtil.getRemainingTimeMillis(token);
        long remainingTimeSeconds = jwtUtil.getRemainingTimeSeconds(token);

        // Then
        assertThat(remainingTimeMillis).isPositive();
        assertThat(remainingTimeSeconds).isPositive();
        assertThat(remainingTimeMillis).isLessThanOrEqualTo(jwtConfig.getAccessTokenExpiration());
        assertThat(remainingTimeSeconds).isEqualTo(remainingTimeMillis / 1000);
    }

    @Test
    void shouldCreateCustomToken() {
        // Given
        String purpose = "password_reset";
        int expirationMinutes = 60;

        // When
        String token = jwtUtil.createCustomToken(testUser, purpose, expirationMinutes);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo(testUser.getEmail());
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(testUser.getId());
        assertThat(jwtUtil.extractTokenType(token)).isEqualTo("custom");
        assertThat(jwtUtil.extractPurpose(token)).isEqualTo(purpose);
    }

    @Test
    void shouldHandleInvalidTokenGracefully() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When & Then
        assertThatThrownBy(() -> jwtUtil.extractUsername(invalidToken))
                .isInstanceOf(JwtException.class);
        
        assertThatThrownBy(() -> jwtUtil.extractAllClaims(invalidToken))
                .isInstanceOf(JwtException.class);
        
        assertThat(jwtUtil.isTokenExpired(invalidToken)).isTrue();
        assertThat(jwtUtil.getRemainingTimeMillis(invalidToken)).isZero();
        assertThat(jwtUtil.validateTokenStructure(invalidToken)).isFalse();
    }

    @Test
    void shouldConvertDateToLocalDateTime() {
        // Given
        Date date = new Date();

        // When
        LocalDateTime localDateTime = jwtUtil.dateToLocalDateTime(date);

        // Then
        assertThat(localDateTime).isNotNull();
    }

    @Test
    void shouldConvertLocalDateTimeToDate() {
        // Given
        LocalDateTime localDateTime = LocalDateTime.now();

        // When
        Date date = jwtUtil.localDateTimeToDate(localDateTime);

        // Then
        assertThat(date).isNotNull();
    }

    @Test
    void shouldGenerateUniqueTokenIds() {
        // Given & When
        String token1 = jwtUtil.generateAccessToken(testUser);
        String token2 = jwtUtil.generateAccessToken(testUser);

        String tokenId1 = jwtUtil.extractTokenId(token1);
        String tokenId2 = jwtUtil.extractTokenId(token2);

        // Then
        assertThat(tokenId1).isNotEqualTo(tokenId2);
        assertThat(tokenId1).isNotNull();
        assertThat(tokenId2).isNotNull();
    }

    @Test
    void shouldHandleNullUser() {
        // When & Then
        assertThatThrownBy(() -> jwtUtil.generateAccessToken(null))
                .isInstanceOf(NullPointerException.class);
    }
}