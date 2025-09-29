package com.tariffsheriff.backend.monitoring;

import com.tariffsheriff.backend.security.jwt.JwtUtil;
import com.tariffsheriff.backend.security.redis.BlacklistService;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for authentication services
 */
@Component
public class AuthenticationHealthIndicator implements HealthIndicator {

    private final JwtUtil jwtUtil;
    private final BlacklistService blacklistService;

    public AuthenticationHealthIndicator(JwtUtil jwtUtil, BlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    public Health health() {
        try {
            // Test JWT functionality
            boolean jwtHealthy = testJwtHealth();
            
            // Test token blacklist functionality
            boolean blacklistHealthy = testBlacklistHealth();
            
            if (jwtHealthy && blacklistHealthy) {
                return Health.up()
                    .withDetail("jwt", "operational")
                    .withDetail("tokenBlacklist", "operational")
                    .withDetail("message", "Authentication services are healthy")
                    .build();
            } else {
                return Health.down()
                    .withDetail("jwt", jwtHealthy ? "operational" : "failed")
                    .withDetail("tokenBlacklist", blacklistHealthy ? "operational" : "failed")
                    .withDetail("message", "Authentication services have issues")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("message", "Authentication health check failed")
                .build();
        }
    }

    private boolean testJwtHealth() {
        try {
            // Test JWT secret availability and basic functionality
            String testToken = jwtUtil.generateTestToken();
            return jwtUtil.validateToken(testToken);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testBlacklistHealth() {
        try {
            // Test Redis connectivity for token blacklisting
            String testTokenId = "health-check-token-" + System.currentTimeMillis();
            blacklistService.blacklistToken(testTokenId, 60L, "health-check");
            boolean isBlacklisted = blacklistService.isTokenBlacklisted(testTokenId);
            
            // Clean up test token
            if (isBlacklisted) {
                // Token will expire automatically due to TTL
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}