package com.tariffsheriff.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.user.dto.LoginRequest;
import com.tariffsheriff.backend.user.dto.RegisterRequest;
import com.tariffsheriff.backend.user.dto.AuthResponse;
import com.tariffsheriff.backend.user.dto.RefreshTokenRequest;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.repository.UserRepository;
import com.tariffsheriff.backend.security.jwt.TokenService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@Transactional
class AuthenticationIntegrationTest {

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
    void shouldRegisterNewUserSuccessfully() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setName("New User");
        request.setPassword("NewPassword123!");
        request.setRole("USER");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully. Please check your email for verification."));

        // Verify user was created in database
        User createdUser = userRepository.findByEmail("newuser@example.com").orElse(null);
        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(createdUser.isEmailVerified()).isFalse();
        assertThat(createdUser.getVerificationToken()).isNotNull();
    }

    @Test
    void shouldRejectRegistrationWithExistingEmail() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(testEmail); // Already exists
        request.setName("Another User");
        request.setPassword("AnotherPassword123!");
        request.setRole("USER");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void shouldRejectRegistrationWithWeakPassword() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("weakpass@example.com");
        request.setName("Weak Pass User");
        request.setPassword("weak"); // Too weak
        request.setRole("USER");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password does not meet security requirements"));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        // When
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andReturn();

        // Verify tokens are valid
        String responseBody = result.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
        
        assertThat(tokenService.validateToken(authResponse.getAccessToken())).isTrue();
        assertThat(tokenService.validateToken(authResponse.getRefreshToken())).isTrue();
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void shouldRejectLoginForUnverifiedUser() throws Exception {
        // Given
        testUser.setEmailVerified(false);
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Please verify your email before logging in"));
    }

    @Test
    void shouldRejectLoginForSuspendedUser() throws Exception {
        // Given
        testUser.setStatus(UserStatus.SUSPENDED);
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is not active"));
    }

    @Test
    void shouldRefreshTokensSuccessfully() throws Exception {
        // Given - First login to get tokens
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // When - Refresh tokens
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(loginResponse.getRefreshToken());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        // Then - Verify new tokens are different and valid
        AuthResponse refreshResponse = objectMapper.readValue(
            refreshResult.getResponse().getContentAsString(), AuthResponse.class);

        assertThat(refreshResponse.getAccessToken()).isNotEqualTo(loginResponse.getAccessToken());
        assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(loginResponse.getRefreshToken());
        assertThat(tokenService.validateToken(refreshResponse.getAccessToken())).isTrue();
        assertThat(tokenService.validateToken(refreshResponse.getRefreshToken())).isTrue();

        // Old refresh token should be blacklisted
        assertThat(tokenService.validateToken(loginResponse.getRefreshToken())).isFalse();
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        // Given - First login to get tokens
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // When - Logout
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        // Then - Tokens should be blacklisted
        assertThat(tokenService.validateToken(loginResponse.getAccessToken())).isFalse();
    }

    @Test
    void shouldAccessProtectedEndpointWithValidToken() throws Exception {
        // Given - Login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(testEmail);
        loginRequest.setPassword(testPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse loginResponse = objectMapper.readValue(
            loginResult.getResponse().getContentAsString(), AuthResponse.class);

        // When & Then - Access protected endpoint
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + loginResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void shouldRejectAccessToProtectedEndpointWithoutToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAccessToProtectedEndpointWithInvalidToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLockAccountAfterMultipleFailedAttempts() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword");

        // When - Make 5 failed login attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // Then - Account should be locked
        User lockedUser = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(lockedUser).isNotNull();
        assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(lockedUser.getAccountLockedUntil()).isAfter(LocalDateTime.now());

        // Further login attempts should be blocked even with correct password
        request.setPassword(testPassword);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is locked"));
    }

    @Test
    void shouldVerifyEmailSuccessfully() throws Exception {
        // Given
        testUser.setStatus(UserStatus.PENDING);
        testUser.setEmailVerified(false);
        testUser.setVerificationToken("test-verification-token");
        testUser.setVerificationTokenExpires(LocalDateTime.now().plusHours(1));
        userRepository.save(testUser);

        // When & Then
        mockMvc.perform(get("/api/auth/verify")
                .param("token", "test-verification-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        // Verify user is activated
        User verifiedUser = userRepository.findByEmail(testEmail).orElse(null);
        assertThat(verifiedUser).isNotNull();
        assertThat(verifiedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(verifiedUser.isEmailVerified()).isTrue();
        assertThat(verifiedUser.getVerificationToken()).isNull();
    }

    @Test
    void shouldRejectExpiredVerificationToken() throws Exception {
        // Given
        testUser.setStatus(UserStatus.PENDING);
        testUser.setEmailVerified(false);
        testUser.setVerificationToken("expired-token");
        testUser.setVerificationTokenExpires(LocalDateTime.now().minusHours(1)); // Expired
        userRepository.save(testUser);

        // When & Then
        mockMvc.perform(get("/api/auth/verify")
                .param("token", "expired-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));
    }
}