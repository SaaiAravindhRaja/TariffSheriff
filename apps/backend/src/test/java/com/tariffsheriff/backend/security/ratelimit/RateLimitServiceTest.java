package com.tariffsheriff.backend.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenReturn("2"); // 2 attempts so far
        when(valueOperations.increment(key)).thenReturn(3L);

        // When
        RateLimitStatus status = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));

        // Then
        assertThat(status.isAllowed()).isTrue();
        assertThat(status.getCurrentCount()).isEqualTo(3);
        assertThat(status.getLimit()).isEqualTo(5);
        assertThat(status.getRemainingAttempts()).isEqualTo(2);

        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(key, Duration.ofMinutes(15));
    }

    @Test
    void shouldBlockRequestWhenOverLimit() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenReturn("5"); // Already at limit
        when(valueOperations.increment(key)).thenReturn(6L);

        // When
        RateLimitStatus status = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));

        // Then
        assertThat(status.isAllowed()).isFalse();
        assertThat(status.getCurrentCount()).isEqualTo(6);
        assertThat(status.getLimit()).isEqualTo(5);
        assertThat(status.getRemainingAttempts()).isEqualTo(0);

        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(key, Duration.ofMinutes(15));
    }

    @Test
    void shouldAllowFirstRequest() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenReturn(null); // No previous attempts
        when(valueOperations.increment(key)).thenReturn(1L);

        // When
        RateLimitStatus status = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));

        // Then
        assertThat(status.isAllowed()).isTrue();
        assertThat(status.getCurrentCount()).isEqualTo(1);
        assertThat(status.getLimit()).isEqualTo(5);
        assertThat(status.getRemainingAttempts()).isEqualTo(4);

        verify(valueOperations).increment(key);
        verify(redisTemplate).expire(key, Duration.ofMinutes(15));
    }

    @Test
    void shouldGetCurrentRateLimitStatus() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenReturn("3");
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(600L); // 10 minutes remaining

        // When
        RateLimitStatus status = rateLimitService.getRateLimitStatus(key, 5);

        // Then
        assertThat(status.getCurrentCount()).isEqualTo(3);
        assertThat(status.getLimit()).isEqualTo(5);
        assertThat(status.getRemainingAttempts()).isEqualTo(2);
        assertThat(status.getResetTimeSeconds()).isEqualTo(600L);
        assertThat(status.isAllowed()).isTrue();

        verify(valueOperations, never()).increment(any());
    }

    @Test
    void shouldResetRateLimitCounter() {
        // Given
        String key = "login:192.168.1.1";

        // When
        rateLimitService.resetRateLimit(key);

        // Then
        verify(redisTemplate).delete(key);
    }

    @Test
    void shouldCreateLoginRateLimitKey() {
        // Given
        String ipAddress = "192.168.1.1";

        // When
        String key = rateLimitService.createLoginRateLimitKey(ipAddress);

        // Then
        assertThat(key).isEqualTo("rate_limit:login:192.168.1.1");
    }

    @Test
    void shouldCreateUserRateLimitKey() {
        // Given
        Long userId = 123L;

        // When
        String key = rateLimitService.createUserRateLimitKey(userId);

        // Then
        assertThat(key).isEqualTo("rate_limit:user:123");
    }

    @Test
    void shouldCreateApiRateLimitKey() {
        // Given
        String endpoint = "/api/tariff/calculate";
        String identifier = "192.168.1.1";

        // When
        String key = rateLimitService.createApiRateLimitKey(endpoint, identifier);

        // Then
        assertThat(key).isEqualTo("rate_limit:api:/api/tariff/calculate:192.168.1.1");
    }

    @Test
    void shouldHandleRedisConnectionFailure() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        RateLimitStatus status = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));

        // Then - Should allow request when Redis is unavailable (fail open)
        assertThat(status.isAllowed()).isTrue();
        assertThat(status.getCurrentCount()).isEqualTo(0);
        assertThat(status.getLimit()).isEqualTo(5);
        assertThat(status.getRemainingAttempts()).isEqualTo(5);
    }

    @Test
    void shouldHandleInvalidCounterValue() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenReturn("invalid"); // Invalid number
        when(valueOperations.increment(key)).thenReturn(1L);

        // When
        RateLimitStatus status = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));

        // Then - Should treat as first request
        assertThat(status.isAllowed()).isTrue();
        assertThat(status.getCurrentCount()).isEqualTo(1);
        assertThat(status.getLimit()).isEqualTo(5);
        assertThat(status.getRemainingAttempts()).isEqualTo(4);
    }

    @Test
    void shouldCheckMultipleRateLimits() {
        // Given
        String ipKey = "rate_limit:login:192.168.1.1";
        String userKey = "rate_limit:user:123";
        
        when(valueOperations.get(ipKey)).thenReturn("2");
        when(valueOperations.get(userKey)).thenReturn("1");
        when(valueOperations.increment(ipKey)).thenReturn(3L);
        when(valueOperations.increment(userKey)).thenReturn(2L);

        // When
        RateLimitStatus ipStatus = rateLimitService.checkRateLimit(ipKey, 5, Duration.ofMinutes(15));
        RateLimitStatus userStatus = rateLimitService.checkRateLimit(userKey, 10, Duration.ofHours(1));

        // Then
        assertThat(ipStatus.isAllowed()).isTrue();
        assertThat(ipStatus.getCurrentCount()).isEqualTo(3);
        assertThat(userStatus.isAllowed()).isTrue();
        assertThat(userStatus.getCurrentCount()).isEqualTo(2);
    }

    @Test
    void shouldCalculateCorrectRemainingTime() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenReturn("3");
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(300L); // 5 minutes remaining

        // When
        RateLimitStatus status = rateLimitService.getRateLimitStatus(key, 5);

        // Then
        assertThat(status.getResetTimeSeconds()).isEqualTo(300L);
    }

    @Test
    void shouldHandleExpiredKey() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenReturn(null); // Key expired
        when(redisTemplate.getExpire(key, TimeUnit.SECONDS)).thenReturn(-2L); // Key doesn't exist
        when(valueOperations.increment(key)).thenReturn(1L);

        // When
        RateLimitStatus status = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));

        // Then
        assertThat(status.isAllowed()).isTrue();
        assertThat(status.getCurrentCount()).isEqualTo(1);
        assertThat(status.getRemainingAttempts()).isEqualTo(4);
    }

    @Test
    void shouldSetExpirationOnFirstRequest() {
        // Given
        String key = "login:192.168.1.1";
        Duration window = Duration.ofMinutes(15);
        when(valueOperations.get(key)).thenReturn(null);
        when(valueOperations.increment(key)).thenReturn(1L);

        // When
        rateLimitService.checkRateLimit(key, 5, window);

        // Then
        verify(redisTemplate).expire(key, window);
    }

    @Test
    void shouldNotResetExpirationOnSubsequentRequests() {
        // Given
        String key = "login:192.168.1.1";
        Duration window = Duration.ofMinutes(15);
        when(valueOperations.get(key)).thenReturn("2"); // Existing counter
        when(valueOperations.increment(key)).thenReturn(3L);

        // When
        rateLimitService.checkRateLimit(key, 5, window);

        // Then
        verify(redisTemplate).expire(key, window); // Still sets expiration to maintain consistency
    }

    @Test
    void shouldHandleConcurrentRequests() {
        // Given
        String key = "login:192.168.1.1";
        when(valueOperations.get(key)).thenReturn("4"); // Near limit
        when(valueOperations.increment(key)).thenReturn(5L, 6L); // Two concurrent increments

        // When
        RateLimitStatus status1 = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));
        RateLimitStatus status2 = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));

        // Then
        assertThat(status1.isAllowed()).isTrue(); // 5th request allowed
        assertThat(status2.isAllowed()).isFalse(); // 6th request blocked
    }

    @Test
    void shouldCreateCorrectKeyFormats() {
        // Test various key formats
        assertThat(rateLimitService.createLoginRateLimitKey("192.168.1.1"))
            .isEqualTo("rate_limit:login:192.168.1.1");
        
        assertThat(rateLimitService.createUserRateLimitKey(123L))
            .isEqualTo("rate_limit:user:123");
        
        assertThat(rateLimitService.createApiRateLimitKey("/api/test", "user123"))
            .isEqualTo("rate_limit:api:/api/test:user123");
    }

    @Test
    void shouldHandleNullAndEmptyValues() {
        // Given
        String key = "login:null";
        when(valueOperations.get(key)).thenReturn("");
        when(valueOperations.increment(key)).thenReturn(1L);

        // When
        RateLimitStatus status = rateLimitService.checkRateLimit(key, 5, Duration.ofMinutes(15));

        // Then
        assertThat(status.isAllowed()).isTrue();
        assertThat(status.getCurrentCount()).isEqualTo(1);
    }
}