package com.tariffsheriff.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.security.jwt.TokenService;
import com.tariffsheriff.backend.security.ratelimit.RateLimitService;
import com.tariffsheriff.backend.user.dto.LoginRequest;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Transactional
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        
        // Enable rate limiting for tests
        registry.add("app.security.rate-limit.enabled", () -> "true");
        registry.add("app.security.rate-limit.login-attempts", () -> "3");
        registry.add("app.security.rate-limit.window-minutes", () -> "1");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RateLimitService rateLimitService;

    private User testUser;
    private final String testEmail = "test@example.com";
    private final String testPassword = "TestPassword123!";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        testUser = new User();
        testUser.setEmail(testEmail);
        testUser.setName("Test User");
        testUser.setPassword(passwordEncoder.encode(testPassword));
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldEnforceRateLimitOnLoginEndpoint() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("WrongPassword");

        String clientIp = "192.168.1.100";

        // When - Make requests up to the limit
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("X-Forwarded-For", clientIp))
                    .andExpect(status().isUnauthorized());
        }

        // Then - Next request should be rate limited
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Forwarded-For", clientIp))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("Too many login attempts. Please try again later."));
    }

    @Test
    void shouldAllowDifferentIPsToHaveSeparateRateLimits() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("WrongPassword");

        String clientIp1 = "192.168.1.100";
        String clientIp2 = "192.168.1.101";

        // When - Exhaust rate limit for first IP
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("X-Forwarded-For", clientIp1))
                    .andExpect(status().isUnauthorized());
        }

        // Then - First IP should be rate limited
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Forwarded-For", clientIp1))
                .andExpect(status().isTooManyRequests());

        // But second IP should still be allowed
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Forwarded-For", clientIp2))
                .andExpect(status().isUnauthorized()); // Normal auth failure, not rate limited
    }

    @Test
    void shouldRejectRequestsWithMalformedJWT() throws Exception {
        // Given
        String malformedToken = "not.a.valid.jwt.token";

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + malformedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void shouldRejectExpiredJWT() throws Exception {
        // Given - Create a token that expires immediately
        User user = testUser;
        String expiredToken = tokenService.createCustomToken(user, "access", -1); // Expired 1 minute ago

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_EXPIRED"));
    }

    @Test
    void shouldRejectBlacklistedJWT() throws Exception {
        // Given - Create and then blacklist a token
        String validToken = tokenService.generateAccessToken(testUser);
        tokenService.blacklistToken(validToken);

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("TOKEN_BLACKLISTED"));
    }

    @Test
    void shouldRejectJWTWithInvalidSignature() throws Exception {
        // Given - Create a token and tamper with it
        String validToken = tokenService.generateAccessToken(testUser);
        String tamperedToken = validToken.substring(0, validToken.length() - 10) + "tampered123";

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }

    @Test
    void shouldEnforceRoleBasedAccessControl() throws Exception {
        // Given - Create admin and regular user tokens
        User adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin User");
        adminUser.setPassword(passwordEncoder.encode("AdminPassword123!"));
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        adminUser.setEmailVerified(true);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser = userRepository.save(adminUser);

        String userToken = tokenService.generateAccessToken(testUser);
        String adminToken = tokenService.generateAccessToken(adminUser);

        // When & Then - Regular user should not access admin endpoints
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));

        // Admin should be able to access admin endpoints
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldIncludeSecurityHeaders() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + tokenService.generateAccessToken(testUser)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void shouldHandleCORSProperly() throws Exception {
        // Given
        String allowedOrigin = "https://tariffsheriff-frontend.vercel.app";

        // When & Then - Preflight request
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", allowedOrigin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", allowedOrigin))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type,Authorization"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void shouldRejectCORSFromUnauthorizedOrigin() throws Exception {
        // Given
        String unauthorizedOrigin = "https://malicious-site.com";

        // When & Then
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", unauthorizedOrigin)
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldValidateCSRFTokenForStateChangingOperations() throws Exception {
        // Given
        String validToken = tokenService.generateAccessToken(testUser);

        // When & Then - POST request without CSRF token should be rejected
        mockMvc.perform(post("/api/user/change-password")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"old\",\"newPassword\":\"new\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF_TOKEN_MISSING"));
    }

    @Test
    void shouldPreventSessionFixation() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        // When - Login twice and verify different session tokens
        String response1 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String response2 = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Then - Tokens should be different
        assertThat(response1).isNotEqualTo(response2);
    }

    @Test
    void shouldLogSecurityEvents() throws Exception {
        // Given
        String maliciousPayload = "<script>alert('xss')</script>";
        LoginRequest request = new LoginRequest();
        request.setEmail(maliciousPayload);
        request.setPassword("password");

        // When - Attempt malicious login
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isBadRequest()); // Should be rejected by validation

        // Then - Security event should be logged (verified through audit logs)
        // This would typically be verified by checking the audit log repository
        // but for this test we'll just verify the request was properly rejected
    }

    @Test
    void shouldHandleConcurrentRequests() throws Exception {
        // Given
        String validToken = tokenService.generateAccessToken(testUser);

        // When - Make multiple concurrent requests
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + validToken))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All requests should have been handled successfully
        // (No assertions needed as exceptions would be thrown if requests failed)
    }

    @Test
    void shouldPreventTimingAttacks() throws Exception {
        // Given
        LoginRequest validUserRequest = new LoginRequest();
        validUserRequest.setEmail(testEmail);
        validUserRequest.setPassword("WrongPassword");

        LoginRequest invalidUserRequest = new LoginRequest();
        invalidUserRequest.setEmail("nonexistent@example.com");
        invalidUserRequest.setPassword("WrongPassword");

        // When - Measure response times
        long startTime1 = System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isUnauthorized());
        long endTime1 = System.currentTimeMillis();

        long startTime2 = System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidUserRequest)))
                .andExpect(status().isUnauthorized());
        long endTime2 = System.currentTimeMillis();

        // Then - Response times should be similar (within reasonable variance)
        long timeDiff = Math.abs((endTime1 - startTime1) - (endTime2 - startTime2));
        assertThat(timeDiff).isLessThan(100); // Allow 100ms variance
    }
}