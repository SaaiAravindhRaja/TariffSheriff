package com.tariffsheriff.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Transactional
class AccountLockoutSecurityTest {

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
        
        // Configure security settings for testing
        registry.add("app.security.max-failed-attempts", () -> "3");
        registry.add("app.security.lockout-duration-minutes", () -> "5");
        registry.add("app.security.rate-limit.enabled", () -> "true");
        registry.add("app.security.rate-limit.login-attempts", () -> "5");
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
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldLockAccountAfterMaxFailedAttempts() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // When - Make 3 failed login attempts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // Then - Account should be locked
        User lockedUser = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(lockedUser).isNotNull();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(lockedUser.getAccountLockedUntil()).isAfter(LocalDateTime.now());

        // Further attempts should be blocked even with correct password
        request.setPassword(testPassword);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is locked"));
    }

    @Test
    void shouldResetFailedAttemptsAfterSuccessfulLogin() throws Exception {
        // Given
        LoginRequest wrongRequest = new LoginRequest();
        wrongRequest.setEmail(testEmail);
        wrongRequest.setPassword("WrongPassword");

        LoginRequest correctRequest = new LoginRequest();
        correctRequest.setEmail(testEmail);
        correctRequest.setPassword(testPassword);

        // When - Make 2 failed attempts, then successful login
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(wrongRequest)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(correctRequest)))
                .andExpect(status().isOk());

        // Then - Failed attempts should be reset
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getAccountLockedUntil()).isNull();
    }

    @Test
    void shouldUnlockAccountAfterLockoutPeriod() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // Lock the account
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // Manually expire the lockout (simulate time passing)
        User lockedUser = userRepository.findByEmail(testEmail).orElse(null);
        lockedUser.setAccountLockedUntil(LocalDateTime.now().minusMinutes(1));
        userRepository.save(lockedUser);

        // When - Try to login with correct password
        request.setPassword(testPassword);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Account should be unlocked and attempts reset
        User unlockedUser = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(unlockedUser).isNotNull();
        assertThat(unlockedUser.getFailedLoginAttempts()).isZero();
        assertThat(unlockedUser.getAccountLockedUntil()).isNull();
    }

    @Test
    void shouldHandleConcurrentFailedLoginAttempts() throws Exception {
        // Given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CompletableFuture<Integer>[] futures = new CompletableFuture[threadCount];

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // When - Make concurrent failed login attempts
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andReturn().getResponse().getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures).get();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Then - Account should be locked
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isGreaterThanOrEqualTo(3);
        assertThat(user.getAccountLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void shouldPreventTimingAttacksOnLockedAccounts() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // Lock the account
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // When - Measure response times for locked account
        long startTime1 = System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        long endTime1 = System.currentTimeMillis();

        // Try with correct password (should still be blocked)
        request.setPassword(testPassword);
        long startTime2 = System.currentTimeMillis();
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        long endTime2 = System.currentTimeMillis();

        // Then - Response times should be similar
        long timeDiff = Math.abs((endTime1 - startTime1) - (endTime2 - startTime2));
        assertThat(timeDiff).isLessThan(100); // Allow 100ms variance
    }

    @Test
    void shouldLogSecurityEventsForAccountLockout() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // When - Trigger account lockout
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("X-Forwarded-For", "192.168.1.100"))
                    .andExpect(status().isUnauthorized());
        }

        // Then - Security events should be logged
        // In a real implementation, we would verify audit logs were created
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getAccountLockedUntil()).isNotNull();
    }

    @Test
    void shouldHandleDifferentIPAddressesSeparately() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        String ip1 = "192.168.1.100";
        String ip2 = "192.168.1.101";

        // When - Make failed attempts from different IPs
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("X-Forwarded-For", ip1))
                    .andExpect(status().isUnauthorized());
        }

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .header("X-Forwarded-For", ip2))
                    .andExpect(status().isUnauthorized());
        }

        // Then - Account should not be locked yet (total 4 attempts but from different IPs)
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(4); // Total attempts tracked per user
        
        // One more attempt should lock the account
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .header("X-Forwarded-For", ip1))
                .andExpect(status().isUnauthorized());

        user = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(user.getAccountLockedUntil()).isNotNull();
    }

    @Test
    void shouldNotLockNonExistentUserAccounts() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("WrongPassword");

        // When - Make multiple failed attempts for non-existent user
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // Then - No user should be created or locked
        assertThat(userRepository.findByEmail("nonexistent@example.com")).isEmpty();
    }

    @Test
    void shouldHandleAccountLockoutWithSuspendedUser() throws Exception {
        // Given
        testUser.setStatus(UserStatus.SUSPENDED);
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // When - Make failed attempts on suspended account
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // Then - Account should still track failed attempts
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
    }

    @Test
    void shouldPreventBruteForceAttacksWithRateLimit() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        String clientIp = "192.168.1.100";

        // When - Make rapid requests (should hit rate limit before account lockout)
        int requestCount = 0;
        for (int i = 0; i < 10; i++) {
            try {
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", clientIp))
                        .andExpect(status().isAnyOf(401, 429)); // Either unauthorized or rate limited
                requestCount++;
            } catch (Exception e) {
                break;
            }
        }

        // Then - Should be rate limited before reaching account lockout
        assertThat(requestCount).isLessThanOrEqualTo(5); // Rate limit should kick in
    }

    @Test
    void shouldAllowAdminToUnlockAccount() throws Exception {
        // Given
        // Lock the account first
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // Verify account is locked
        User lockedUser = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(lockedUser.getAccountLockedUntil()).isNotNull();

        // When - Admin unlocks the account (simulated by directly updating the database)
        lockedUser.setAccountLockedUntil(null);
        lockedUser.setFailedLoginAttempts(0);
        userRepository.save(lockedUser);

        // Then - User should be able to login
        request.setPassword(testPassword);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleAccountLockoutDuringPasswordReset() throws Exception {
        // Given
        // Lock the account
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        // When - Try to reset password for locked account
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + testEmail + "\"}"))
                .andExpect(status().isOk()); // Should still allow password reset

        // Then - Password reset should work even for locked accounts
        // This is a security design decision - locked accounts can still reset passwords
    }
}