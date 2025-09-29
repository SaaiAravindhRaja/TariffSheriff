package com.tariffsheriff.backend.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting service using Redis for authentication endpoints.
 * Implements IP-based and user-based rate limiting with configurable limits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    // Rate limiting configuration
    private static final int LOGIN_ATTEMPTS_LIMIT = 5;
    private static final int LOGIN_ATTEMPTS_WINDOW_MINUTES = 15;
    private static final int REGISTRATION_LIMIT = 3;
    private static final int REGISTRATION_WINDOW_MINUTES = 60;
    private static final int PASSWORD_RESET_LIMIT = 3;
    private static final int PASSWORD_RESET_WINDOW_MINUTES = 60;
    private static final int IP_GLOBAL_LIMIT = 100;
    private static final int IP_GLOBAL_WINDOW_MINUTES = 60;

    // Redis key prefixes
    private static final String LOGIN_ATTEMPTS_PREFIX = "rate_limit:login:";
    private static final String REGISTRATION_PREFIX = "rate_limit:register:";
    private static final String PASSWORD_RESET_PREFIX = "rate_limit:password_reset:";
    private static final String IP_GLOBAL_PREFIX = "rate_limit:ip:";
    private static final String USER_LOCKOUT_PREFIX = "user_lockout:";

    /**
     * Check if login attempts are within rate limit for IP address.
     *
     * @param ipAddress Client IP address
     * @return true if within limit, false if rate limited
     */
    public boolean isLoginAllowed(String ipAddress) {
        String key = LOGIN_ATTEMPTS_PREFIX + ipAddress;
        return checkRateLimit(key, LOGIN_ATTEMPTS_LIMIT, LOGIN_ATTEMPTS_WINDOW_MINUTES);
    }

    /**
     * Record a login attempt for IP address.
     *
     * @param ipAddress Client IP address
     * @return current attempt count
     */
    public int recordLoginAttempt(String ipAddress) {
        String key = LOGIN_ATTEMPTS_PREFIX + ipAddress;
        return incrementCounter(key, LOGIN_ATTEMPTS_WINDOW_MINUTES);
    }

    /**
     * Check if registration is allowed for IP address.
     *
     * @param ipAddress Client IP address
     * @return true if within limit, false if rate limited
     */
    public boolean isRegistrationAllowed(String ipAddress) {
        String key = REGISTRATION_PREFIX + ipAddress;
        return checkRateLimit(key, REGISTRATION_LIMIT, REGISTRATION_WINDOW_MINUTES);
    }

    /**
     * Record a registration attempt for IP address.
     *
     * @param ipAddress Client IP address
     * @return current attempt count
     */
    public int recordRegistrationAttempt(String ipAddress) {
        String key = REGISTRATION_PREFIX + ipAddress;
        return incrementCounter(key, REGISTRATION_WINDOW_MINUTES);
    }

    /**
     * Check if password reset is allowed for IP address.
     *
     * @param ipAddress Client IP address
     * @return true if within limit, false if rate limited
     */
    public boolean isPasswordResetAllowed(String ipAddress) {
        String key = PASSWORD_RESET_PREFIX + ipAddress;
        return checkRateLimit(key, PASSWORD_RESET_LIMIT, PASSWORD_RESET_WINDOW_MINUTES);
    }

    /**
     * Record a password reset attempt for IP address.
     *
     * @param ipAddress Client IP address
     * @return current attempt count
     */
    public int recordPasswordResetAttempt(String ipAddress) {
        String key = PASSWORD_RESET_PREFIX + ipAddress;
        return incrementCounter(key, PASSWORD_RESET_WINDOW_MINUTES);
    }

    /**
     * Check global rate limit for IP address across all endpoints.
     *
     * @param ipAddress Client IP address
     * @return true if within limit, false if rate limited
     */
    public boolean isIpAllowed(String ipAddress) {
        String key = IP_GLOBAL_PREFIX + ipAddress;
        return checkRateLimit(key, IP_GLOBAL_LIMIT, IP_GLOBAL_WINDOW_MINUTES);
    }

    /**
     * Record a request for IP address global rate limiting.
     *
     * @param ipAddress Client IP address
     * @return current request count
     */
    public int recordIpRequest(String ipAddress) {
        String key = IP_GLOBAL_PREFIX + ipAddress;
        return incrementCounter(key, IP_GLOBAL_WINDOW_MINUTES);
    }

    /**
     * Lock user account temporarily due to suspicious activity.
     *
     * @param userId User ID to lock
     * @param lockDurationMinutes Duration to lock account in minutes
     */
    public void lockUserAccount(Long userId, int lockDurationMinutes) {
        String key = USER_LOCKOUT_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "locked", Duration.ofMinutes(lockDurationMinutes));
        log.warn("User account locked: {} for {} minutes", userId, lockDurationMinutes);
    }

    /**
     * Check if user account is temporarily locked.
     *
     * @param userId User ID to check
     * @return true if account is locked, false otherwise
     */
    public boolean isUserAccountLocked(Long userId) {
        String key = USER_LOCKOUT_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null;
    }

    /**
     * Unlock user account by removing the lock.
     *
     * @param userId User ID to unlock
     */
    public void unlockUserAccount(Long userId) {
        String key = USER_LOCKOUT_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("User account unlocked: {}", userId);
    }

    /**
     * Get remaining time for user account lock in seconds.
     *
     * @param userId User ID to check
     * @return remaining lock time in seconds, or 0 if not locked
     */
    public long getUserLockRemainingTime(Long userId) {
        String key = USER_LOCKOUT_PREFIX + userId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * Clear all rate limiting data for an IP address (admin function).
     *
     * @param ipAddress IP address to clear
     */
    public void clearIpRateLimits(String ipAddress) {
        redisTemplate.delete(LOGIN_ATTEMPTS_PREFIX + ipAddress);
        redisTemplate.delete(REGISTRATION_PREFIX + ipAddress);
        redisTemplate.delete(PASSWORD_RESET_PREFIX + ipAddress);
        redisTemplate.delete(IP_GLOBAL_PREFIX + ipAddress);
        log.info("Cleared rate limits for IP: {}", ipAddress);
    }

    /**
     * Get current attempt count for a specific rate limit key.
     *
     * @param key Redis key
     * @return current attempt count
     */
    public int getCurrentAttemptCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Integer.parseInt(value) : 0;
    }

    /**
     * Get remaining time for rate limit reset in seconds.
     *
     * @param key Redis key
     * @return remaining time in seconds, or 0 if no limit active
     */
    public long getRateLimitRemainingTime(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * Check if request is within rate limit.
     *
     * @param key Redis key for the rate limit
     * @param limit Maximum allowed attempts
     * @param windowMinutes Time window in minutes
     * @return true if within limit, false if exceeded
     */
    private boolean checkRateLimit(String key, int limit, int windowMinutes) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return true; // No previous attempts
            }
            
            int currentCount = Integer.parseInt(value);
            boolean allowed = currentCount < limit;
            
            if (!allowed) {
                log.debug("Rate limit exceeded for key: {} (count: {}, limit: {})", key, currentCount, limit);
            }
            
            return allowed;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            return true; // Allow request if Redis is unavailable
        }
    }

    /**
     * Increment counter for rate limiting.
     *
     * @param key Redis key
     * @param windowMinutes Time window in minutes
     * @return new counter value
     */
    private int incrementCounter(String key, int windowMinutes) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                // First attempt, set counter to 1 with expiration
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(windowMinutes));
                return 1;
            } else {
                // Increment existing counter
                Long newValue = redisTemplate.opsForValue().increment(key);
                return newValue != null ? newValue.intValue() : 1;
            }
        } catch (Exception e) {
            log.error("Error incrementing counter for key: {}", key, e);
            return 1; // Return 1 if Redis is unavailable
        }
    }

    /**
     * Check for suspicious activity patterns.
     *
     * @param ipAddress Client IP address
     * @param userId User ID (can be null for anonymous requests)
     * @return true if suspicious activity detected
     */
    public boolean isSuspiciousActivity(String ipAddress, Long userId) {
        // Check if IP has exceeded multiple rate limits
        int loginAttempts = getCurrentAttemptCount(LOGIN_ATTEMPTS_PREFIX + ipAddress);
        int registrationAttempts = getCurrentAttemptCount(REGISTRATION_PREFIX + ipAddress);
        int passwordResetAttempts = getCurrentAttemptCount(PASSWORD_RESET_PREFIX + ipAddress);
        
        // Consider suspicious if multiple limits are being hit
        int limitsHit = 0;
        if (loginAttempts >= LOGIN_ATTEMPTS_LIMIT) limitsHit++;
        if (registrationAttempts >= REGISTRATION_LIMIT) limitsHit++;
        if (passwordResetAttempts >= PASSWORD_RESET_LIMIT) limitsHit++;
        
        boolean suspicious = limitsHit >= 2;
        
        if (suspicious) {
            log.warn("Suspicious activity detected for IP: {} (login: {}, reg: {}, reset: {})", 
                    ipAddress, loginAttempts, registrationAttempts, passwordResetAttempts);
        }
        
        return suspicious;
    }

    /**
     * Check rate limit and return status for a given key.
     * This method is used by tests and provides detailed status information.
     *
     * @param key Redis key for the rate limit
     * @param limit Maximum allowed attempts
     * @param windowDuration Time window duration
     * @return RateLimitStatus object with current status
     */
    public RateLimitStatus checkRateLimit(String key, int limit, Duration windowDuration) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            int currentCount = value != null ? Integer.parseInt(value) : 0;
            
            // Increment the counter
            Long newCount = redisTemplate.opsForValue().increment(key);
            if (newCount != null && newCount == 1) {
                // First request, set expiration
                redisTemplate.expire(key, windowDuration);
            }
            
            currentCount = newCount != null ? newCount.intValue() : 1;
            boolean allowed = currentCount <= limit;
            int remaining = Math.max(0, limit - currentCount);
            long resetTime = getRateLimitRemainingTime(key);
            
            return RateLimitStatus.builder()
                    .allowed(allowed)
                    .currentCount(currentCount)
                    .limit(limit)
                    .remainingAttempts(remaining)
                    .resetTimeSeconds(resetTime)
                    .build();
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Return allowed status if Redis is unavailable
            return RateLimitStatus.builder()
                    .allowed(true)
                    .currentCount(0)
                    .limit(limit)
                    .remainingAttempts(limit)
                    .resetTimeSeconds(0)
                    .build();
        }
    }

    /**
     * Reset rate limit for a given key.
     *
     * @param key Redis key to reset
     */
    public void resetRateLimit(String key) {
        redisTemplate.delete(key);
        log.debug("Reset rate limit for key: {}", key);
    }

    /**
     * Create login rate limit key for IP address.
     *
     * @param ipAddress IP address
     * @return Redis key for login rate limiting
     */
    public String createLoginRateLimitKey(String ipAddress) {
        return LOGIN_ATTEMPTS_PREFIX + ipAddress;
    }

    /**
     * Create user rate limit key for user ID.
     *
     * @param userId User ID
     * @return Redis key for user rate limiting
     */
    public String createUserRateLimitKey(Long userId) {
        return "rate_limit:user:" + userId;
    }

    /**
     * Create API rate limit key for IP and endpoint.
     *
     * @param ipAddress IP address
     * @param endpoint API endpoint
     * @return Redis key for API rate limiting
     */
    public String createApiRateLimitKey(String ipAddress, String endpoint) {
        return "rate_limit:api:" + ipAddress + ":" + endpoint;
    }

    /**
     * Get rate limit status for monitoring/debugging.
     *
     * @param ipAddress IP address to check
     * @return RateLimitStatus object with current limits
     */
    public RateLimitStatus getRateLimitStatus(String ipAddress) {
        return RateLimitStatus.builder()
                .ipAddress(ipAddress)
                .loginAttempts(getCurrentAttemptCount(LOGIN_ATTEMPTS_PREFIX + ipAddress))
                .loginAttemptsLimit(LOGIN_ATTEMPTS_LIMIT)
                .loginRemainingTime(getRateLimitRemainingTime(LOGIN_ATTEMPTS_PREFIX + ipAddress))
                .registrationAttempts(getCurrentAttemptCount(REGISTRATION_PREFIX + ipAddress))
                .registrationLimit(REGISTRATION_LIMIT)
                .registrationRemainingTime(getRateLimitRemainingTime(REGISTRATION_PREFIX + ipAddress))
                .passwordResetAttempts(getCurrentAttemptCount(PASSWORD_RESET_PREFIX + ipAddress))
                .passwordResetLimit(PASSWORD_RESET_LIMIT)
                .passwordResetRemainingTime(getRateLimitRemainingTime(PASSWORD_RESET_PREFIX + ipAddress))
                .globalRequests(getCurrentAttemptCount(IP_GLOBAL_PREFIX + ipAddress))
                .globalLimit(IP_GLOBAL_LIMIT)
                .globalRemainingTime(getRateLimitRemainingTime(IP_GLOBAL_PREFIX + ipAddress))
                .build();
    }
}