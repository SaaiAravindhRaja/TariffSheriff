package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.security.jwt.TokenService;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.service.AuthenticationService.AuthenticationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;
    
    @Mock
    private TokenService tokenService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuditService auditService;

    private AuthenticationService authenticationService;
    private User testUser;
    private final String testEmail = "test@example.com";
    private final String testPassword = "TestPassword123!";
    private final String testIpAddress = "192.168.1.1";
    private final String testUserAgent = "Mozilla/5.0";

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
            userService, tokenService, passwordEncoder, auditService
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);
        testUser.setName("Test User");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setEmailVerified(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        when(userService.isEmailRegistered(testEmail)).thenReturn(false);
        when(userService.isPasswordValid(testPassword)).thenReturn(true);
        when(userService.createUserWithVerification(anyString(), eq(testEmail), eq(testPassword), eq(UserRole.USER)))
            .thenReturn(testUser);

        // When
        AuthenticationResult result = authenticationService.registerUser(
            "Test User", testEmail, testPassword, UserRole.USER, testIpAddress
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("registered successfully");
        assertThat(result.getUser()).isEqualTo(testUser);
        
        verify(userService).isEmailRegistered(testEmail);
        verify(userService).isPasswordValid(testPassword);
        verify(userService).createUserWithVerification("Test User", testEmail, testPassword, UserRole.USER);
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("USER_REGISTERED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldFailRegistrationWhenEmailAlreadyExists() {
        // Given
        when(userService.isEmailRegistered(testEmail)).thenReturn(true);

        // When
        AuthenticationResult result = authenticationService.registerUser(
            "Test User", testEmail, testPassword, UserRole.USER, testIpAddress
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("already registered");
        
        verify(userService).isEmailRegistered(testEmail);
        verify(userService, never()).createUserWithVerification(anyString(), anyString(), anyString(), any());
        verify(auditService).logAuthenticationEvent(isNull(), eq("REGISTRATION_FAILED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldFailRegistrationWhenPasswordIsWeak() {
        // Given
        when(userService.isEmailRegistered(testEmail)).thenReturn(false);
        when(userService.isPasswordValid(testPassword)).thenReturn(false);

        // When
        AuthenticationResult result = authenticationService.registerUser(
            "Test User", testEmail, testPassword, UserRole.USER, testIpAddress
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("security requirements");
        
        verify(userService).isPasswordValid(testPassword);
        verify(userService, never()).createUserWithVerification(anyString(), anyString(), anyString(), any());
        verify(auditService).logAuthenticationEvent(isNull(), eq("REGISTRATION_FAILED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        // Given
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.canUserLogin(testUser)).thenReturn(true);
        when(passwordEncoder.matches(testPassword, testUser.getPassword())).thenReturn(true);
        when(tokenService.generateAccessToken(testUser)).thenReturn("access-token");
        when(tokenService.generateRefreshToken(testUser)).thenReturn("refresh-token");

        // When
        AuthenticationResult result = authenticationService.authenticateUser(
            testEmail, testPassword, testIpAddress, testUserAgent
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Login successful");
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUser()).isEqualTo(testUser);
        
        verify(userService).updateLastLogin(testUser.getId(), testIpAddress);
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("LOGIN_SUCCESS"), 
            anyString(), eq(testIpAddress), eq(testUserAgent));
    }

    @Test
    void shouldFailAuthenticationWhenUserNotFound() {
        // Given
        when(userService.findByEmail(testEmail)).thenReturn(Optional.empty());

        // When
        AuthenticationResult result = authenticationService.authenticateUser(
            testEmail, testPassword, testIpAddress, testUserAgent
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Invalid credentials");
        
        verify(auditService).logAuthenticationEvent(isNull(), eq("LOGIN_FAILED"), 
            anyString(), eq(testIpAddress), eq(testUserAgent));
    }

    @Test
    void shouldFailAuthenticationWhenUserCannotLogin() {
        // Given
        testUser.setStatus(UserStatus.SUSPENDED);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.canUserLogin(testUser)).thenReturn(false);

        // When
        AuthenticationResult result = authenticationService.authenticateUser(
            testEmail, testPassword, testIpAddress, testUserAgent
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("not active");
        
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("LOGIN_BLOCKED"), 
            anyString(), eq(testIpAddress), eq(testUserAgent));
    }

    @Test
    void shouldFailAuthenticationWhenPasswordIsIncorrect() {
        // Given
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.canUserLogin(testUser)).thenReturn(true);
        when(passwordEncoder.matches(testPassword, testUser.getPassword())).thenReturn(false);

        // When
        AuthenticationResult result = authenticationService.authenticateUser(
            testEmail, testPassword, testIpAddress, testUserAgent
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Invalid credentials");
        
        verify(userService).recordFailedLoginAttempt(testEmail);
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("LOGIN_FAILED"), 
            anyString(), eq(testIpAddress), eq(testUserAgent));
    }

    @Test
    void shouldFailAuthenticationWhenEmailNotVerified() {
        // Given
        testUser.setEmailVerified(false);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.canUserLogin(testUser)).thenReturn(true);
        when(passwordEncoder.matches(testPassword, testUser.getPassword())).thenReturn(true);

        // When
        AuthenticationResult result = authenticationService.authenticateUser(
            testEmail, testPassword, testIpAddress, testUserAgent
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("verify your email");
        
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("LOGIN_BLOCKED"), 
            anyString(), eq(testIpAddress), eq(testUserAgent));
    }

    @Test
    void shouldRefreshTokensSuccessfully() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(tokenService.validateToken(refreshToken)).thenReturn(true);
        when(tokenService.extractUserFromToken(refreshToken)).thenReturn(testUser);
        when(userService.canUserLogin(testUser)).thenReturn(true);
        when(tokenService.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(tokenService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

        // When
        AuthenticationResult result = authenticationService.refreshTokens(refreshToken, testIpAddress);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Tokens refreshed");
        assertThat(result.getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh-token");
        
        verify(tokenService).blacklistToken(refreshToken);
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("TOKEN_REFRESHED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldFailTokenRefreshWhenTokenInvalid() {
        // Given
        String refreshToken = "invalid-refresh-token";
        when(tokenService.validateToken(refreshToken)).thenReturn(false);

        // When
        AuthenticationResult result = authenticationService.refreshTokens(refreshToken, testIpAddress);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Invalid refresh token");
        
        verify(auditService).logAuthenticationEvent(isNull(), eq("TOKEN_REFRESH_FAILED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldLogoutUserSuccessfully() {
        // Given
        String accessToken = "valid-access-token";
        String refreshToken = "valid-refresh-token";
        when(tokenService.validateToken(accessToken)).thenReturn(true);
        when(tokenService.extractUserFromToken(accessToken)).thenReturn(testUser);

        // When
        AuthenticationResult result = authenticationService.logoutUser(
            accessToken, refreshToken, testIpAddress
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Logout successful");
        
        verify(tokenService).blacklistToken(accessToken);
        verify(tokenService).blacklistToken(refreshToken);
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("LOGOUT_SUCCESS"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldVerifyEmailSuccessfully() {
        // Given
        String verificationToken = "valid-verification-token";
        when(userService.activateUser(verificationToken)).thenReturn(true);

        // When
        AuthenticationResult result = authenticationService.verifyEmail(verificationToken, testIpAddress);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Email verified successfully");
        
        verify(auditService).logAuthenticationEvent(isNull(), eq("EMAIL_VERIFIED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldFailEmailVerificationWhenTokenInvalid() {
        // Given
        String verificationToken = "invalid-verification-token";
        when(userService.activateUser(verificationToken)).thenReturn(false);

        // When
        AuthenticationResult result = authenticationService.verifyEmail(verificationToken, testIpAddress);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Invalid or expired");
        
        verify(auditService).logAuthenticationEvent(isNull(), eq("EMAIL_VERIFICATION_FAILED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldInitiatePasswordResetSuccessfully() {
        // When
        AuthenticationResult result = authenticationService.initiatePasswordReset(testEmail, testIpAddress);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("password reset link has been sent");
        
        verify(userService).initiatePasswordReset(testEmail);
        verify(auditService).logAuthenticationEvent(isNull(), eq("PASSWORD_RESET_REQUESTED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldResetPasswordSuccessfully() {
        // Given
        String resetToken = "valid-reset-token";
        String newPassword = "NewPassword123!";
        when(userService.isPasswordValid(newPassword)).thenReturn(true);
        when(userService.resetPassword(resetToken, newPassword)).thenReturn(true);

        // When
        AuthenticationResult result = authenticationService.resetPassword(resetToken, newPassword, testIpAddress);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Password reset successfully");
        
        verify(auditService).logAuthenticationEvent(isNull(), eq("PASSWORD_RESET_SUCCESS"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldFailPasswordResetWhenPasswordIsWeak() {
        // Given
        String resetToken = "valid-reset-token";
        String newPassword = "weak";
        when(userService.isPasswordValid(newPassword)).thenReturn(false);

        // When
        AuthenticationResult result = authenticationService.resetPassword(resetToken, newPassword, testIpAddress);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("security requirements");
        
        verify(auditService).logAuthenticationEvent(isNull(), eq("PASSWORD_RESET_FAILED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        // Given
        String currentPassword = "CurrentPassword123!";
        String newPassword = "NewPassword123!";
        when(userService.isPasswordValid(newPassword)).thenReturn(true);
        when(userService.changePassword(testUser.getId(), currentPassword, newPassword)).thenReturn(true);

        // When
        AuthenticationResult result = authenticationService.changePassword(
            testUser.getId(), currentPassword, newPassword, testIpAddress
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Password changed successfully");
        
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("PASSWORD_CHANGED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldFailPasswordChangeWhenCurrentPasswordIncorrect() {
        // Given
        String currentPassword = "WrongPassword";
        String newPassword = "NewPassword123!";
        when(userService.isPasswordValid(newPassword)).thenReturn(true);
        when(userService.changePassword(testUser.getId(), currentPassword, newPassword)).thenReturn(false);

        // When
        AuthenticationResult result = authenticationService.changePassword(
            testUser.getId(), currentPassword, newPassword, testIpAddress
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Current password is incorrect");
        
        verify(auditService).logAuthenticationEvent(eq(testUser.getId()), eq("PASSWORD_CHANGE_FAILED"), 
            anyString(), eq(testIpAddress), isNull());
    }

    @Test
    void shouldHandleExceptionsGracefully() {
        // Given
        when(userService.isEmailRegistered(testEmail)).thenThrow(new RuntimeException("Database error"));

        // When
        AuthenticationResult result = authenticationService.registerUser(
            "Test User", testEmail, testPassword, UserRole.USER, testIpAddress
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Registration failed");
        
        verify(auditService).logAuthenticationEvent(isNull(), eq("REGISTRATION_ERROR"), 
            anyString(), eq(testIpAddress), isNull());
    }
}