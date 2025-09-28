package com.tariffsheriff.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Security configuration properties for authentication system.
 * Centralizes security-related configuration values.
 */
@Configuration
@Getter
public class SecurityProperties {

    @Value("${security.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${security.account-lockout.enabled:true}")
    private boolean accountLockoutEnabled;

    @Value("${security.account-lockout.max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${security.account-lockout.duration:900000}")
    private long lockoutDuration;

    /**
     * Get allowed origins as an array.
     */
    public String[] getAllowedOriginsArray() {
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            return new String[]{"http://localhost:3000"};
        }
        return allowedOrigins.split(",");
    }

    /**
     * Get lockout duration in seconds.
     */
    public long getLockoutDurationInSeconds() {
        return lockoutDuration / 1000;
    }

    /**
     * Check if rate limiting is enabled.
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    /**
     * Check if account lockout is enabled.
     */
    public boolean isAccountLockoutEnabled() {
        return accountLockoutEnabled;
    }
}