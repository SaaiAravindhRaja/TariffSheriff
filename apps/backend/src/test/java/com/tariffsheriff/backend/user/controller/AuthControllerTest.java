package com.tariffsheriff.backend.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tariffsheriff.backend.user.dto.*;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.service.AuthenticationService;
import com.tariffsheriff.backend.user.service.AuthenticationService.AuthenticationResult;
import com.tariffsheriff.backend.security.jwt.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private TokenService tokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");
        request.setPassword("TestPassword123!");
        request.setRole("USER");

        AuthenticationResult result = AuthenticationResult.success(
            "User registered successfully", null, null, testUser);

        when(authenticationService.registerUser(anyString(), anyString(), anyString(), 
            any(UserRole.class), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));

        verify(authenticationService).registerUser("Test User", "test@example.com", 
            "TestPassword123!", UserRole.USER, anyString());
    }

    @Test
    void shouldRejectRegistrationWithInvalidData() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setName("");
        request.setPassword("weak");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verify(authenticationService, never()).registerUser(anyString(), anyString(), 
            anyString(), any(UserRole.class), anyString());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("TestPassword123!");

        AuthenticationResult result = AuthenticationResult.success(
            "Login successful", "access-token", "refresh-token", testUser);

        when(authenticationService.authenticateUser(anyString(), anyString(), 
            anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));

        verify(authenticationService).authenticateUser(eq("test@example.com"), 
            eq("TestPassword123!"), anyString(), anyString());
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("WrongPassword");

        AuthenticationResult result = AuthenticationResult.failure("Invalid credentials");

        when(authenticationService.authenticateUser(anyString(), anyString(), 
            anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void shouldRefreshTokensSuccessfully() throws Exception {
        // Given
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        AuthenticationResult result = AuthenticationResult.success(
            "Tokens refreshed", "new-access-token", "new-refresh-token", testUser);

        when(authenticationService.refreshTokens(anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        verify(authenticationService).refreshTokens("valid-refresh-token", anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldLogoutSuccessfully() throws Exception {
        // Given
        AuthenticationResult result = AuthenticationResult.success("Logout successful", null, null, null);

        when(tokenService.extractUserFromToken(anyString())).thenReturn(testUser);
        when(authenticationService.logoutUser(anyString(), anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .with(csrf())
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(authenticationService).logoutUser(anyString(), isNull(), anyString());
    }

    @Test
    void shouldVerifyEmailSuccessfully() throws Exception {
        // Given
        AuthenticationResult result = AuthenticationResult.success("Email verified successfully", null, null, null);

        when(authenticationService.verifyEmail(anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/auth/verify")
                .param("token", "verification-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        verify(authenticationService).verifyEmail("verification-token", anyString());
    }

    @Test
    void shouldInitiateForgotPasswordSuccessfully() throws Exception {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        AuthenticationResult result = AuthenticationResult.success(
            "Password reset link sent", null, null, null);

        when(authenticationService.initiatePasswordReset(anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset link sent"));

        verify(authenticationService).initiatePasswordReset("test@example.com", anyString());
    }

    @Test
    void shouldResetPasswordSuccessfully() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("NewPassword123!");

        AuthenticationResult result = AuthenticationResult.success("Password reset successfully", null, null, null);

        when(authenticationService.resetPassword(anyString(), anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        verify(authenticationService).resetPassword("reset-token", "NewPassword123!", anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldChangePasswordSuccessfully() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("CurrentPassword123!");
        request.setNewPassword("NewPassword123!");

        AuthenticationResult result = AuthenticationResult.success("Password changed successfully", null, null, null);

        when(tokenService.extractUserFromToken(anyString())).thenReturn(testUser);
        when(authenticationService.changePassword(anyLong(), anyString(), anyString(), anyString())).thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/change-password")
                .with(csrf())
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));

        verify(authenticationService).changePassword(1L, "CurrentPassword123!", "NewPassword123!", anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldGetCurrentUserSuccessfully() throws Exception {
        // Given
        when(tokenService.extractUserFromToken(anyString())).thenReturn(testUser);

        // When & Then
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldRejectRequestsWithoutCSRFToken() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("TestPassword123!");

        // When & Then - Request without CSRF token should be rejected
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldValidateRequestData() throws Exception {
        // Given - Empty request
        RegisterRequest request = new RegisterRequest();

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void shouldHandleServiceExceptions() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("TestPassword123!");

        when(authenticationService.authenticateUser(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void shouldReturnProperErrorResponseFormat() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid-email");
        request.setPassword("");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void shouldIncludeRateLimitHeaders() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("TestPassword123!");

        AuthenticationResult result = AuthenticationResult.success(
            "Login successful", "access-token", "refresh-token", testUser);

        when(authenticationService.authenticateUser(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }
}