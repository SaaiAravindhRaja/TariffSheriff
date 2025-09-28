package com.tariffsheriff.backend.user.service;

import com.tariffsheriff.backend.security.jwt.TokenService;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {
    
    private final UserService userService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    
    public AuthenticationService(
            UserService userService, 
            TokenService tokenService, 
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * Register a new user
     * Requirements: 1.1, 1.4, 1.5
     */
    public AuthenticationResult registerUser(String name, String email, String password, UserRole role, String ipAddress) {
        try {
            // Check if email is already registered
            if (userService.isEmailRegistered(email)) {
                auditService.logAuthenticationEvent(null, "REGISTRATION_FAILED", 
                    "Email already registered: " + email, ipAddress, null);
                return AuthenticationResult.failure("Email is already registered");
            }

            // Validate password strength
            if (!userService.isPasswordValid(password)) {
                auditService.logAuthenticationEvent(null, "REGISTRATION_FAILED", 
                    "Weak password for email: " + email, ipAddress, null);
                return AuthenticationResult.failure("Password does not meet security requirements");
            }

            // Create user with verification
            User user = userService.createUserWithVerification(name, email, password, role);
            
            auditService.logAuthenticationEvent(user.getId(), "USER_REGISTERED", 
                "User registered successfully", ipAddress, null);
            
            return AuthenticationResult.success("User registered successfully. Please check your email for verification.", null, null, user);
            
        } catch (Exception e) {
            auditService.logAuthenticationEvent(null, "REGISTRATION_ERROR", 
                "Registration error: " + e.getMessage(), ipAddress, null);
            return AuthenticationResult.failure("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate user login
     * Requirements: 2.1, 2.2, 2.6
     */
    public AuthenticationResult authenticateUser(String email, String password, String ipAddress, String userAgent) {
        try {
            Optional<User> userOpt = userService.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                auditService.logAuthenticationEvent(null, "LOGIN_FAILED", 
                    "User not found: " + email, ipAddress, userAgent);
                return AuthenticationResult.failure("Invalid credentials");
            }

            User user = userOpt.get();

            // Check if user can login (status and lockout)
            if (!userService.canUserLogin(user)) {
                String reason = user.isAccountLocked() ? "Account is locked" : "Account is not active";
                auditService.logAuthenticationEvent(user.getId(), "LOGIN_BLOCKED", 
                    reason, ipAddress, userAgent);
                return AuthenticationResult.failure(reason);
            }

            // Verify password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                userService.recordFailedLoginAttempt(email);
                auditService.logAuthenticationEvent(user.getId(), "LOGIN_FAILED", 
                    "Invalid password", ipAddress, userAgent);
                return AuthenticationResult.failure("Invalid credentials");
            }

            // Check if email is verified
            if (!user.isEmailVerified()) {
                auditService.logAuthenticationEvent(user.getId(), "LOGIN_BLOCKED", 
                    "Email not verified", ipAddress, userAgent);
                return AuthenticationResult.failure("Please verify your email before logging in");
            }

            // Generate tokens
            String accessToken = tokenService.generateAccessToken(user);
            String refreshToken = tokenService.generateRefreshToken(user);

            // Update last login
            userService.updateLastLogin(user.getId(), ipAddress);

            auditService.logAuthenticationEvent(user.getId(), "LOGIN_SUCCESS", 
                "User logged in successfully", ipAddress, userAgent);

            return AuthenticationResult.success("Login successful", accessToken, refreshToken, user);

        } catch (Exception e) {
            auditService.logAuthenticationEvent(null, "LOGIN_ERROR", 
                "Login error: " + e.getMessage(), ipAddress, userAgent);
            return AuthenticationResult.failure("Authentication failed");
        }
    }

    /**
     * Refresh authentication tokens
     * Requirements: 3.3, 3.4
     */
    public AuthenticationResult refreshTokens(String refreshToken, String ipAddress) {
        try {
            if (!tokenService.validateToken(refreshToken)) {
                auditService.logAuthenticationEvent(null, "TOKEN_REFRESH_FAILED", 
                    "Invalid refresh token", ipAddress, null);
                return AuthenticationResult.failure("Invalid refresh token");
            }

            User user = tokenService.extractUserFromToken(refreshToken);
            if (user == null || !userService.canUserLogin(user)) {
                auditService.logAuthenticationEvent(user != null ? user.getId() : null, 
                    "TOKEN_REFRESH_BLOCKED", "User cannot login", ipAddress, null);
                return AuthenticationResult.failure("User account is not active");
            }

            // Blacklist old refresh token
            tokenService.blacklistToken(refreshToken);

            // Generate new tokens
            String newAccessToken = tokenService.generateAccessToken(user);
            String newRefreshToken = tokenService.generateRefreshToken(user);

            auditService.logAuthenticationEvent(user.getId(), "TOKEN_REFRESHED", 
                "Tokens refreshed successfully", ipAddress, null);

            return AuthenticationResult.success("Tokens refreshed", newAccessToken, newRefreshToken, user);

        } catch (Exception e) {
            auditService.logAuthenticationEvent(null, "TOKEN_REFRESH_ERROR", 
                "Token refresh error: " + e.getMessage(), ipAddress, null);
            return AuthenticationResult.failure("Token refresh failed");
        }
    }

    /**
     * Logout user
     * Requirements: 2.4
     */
    public AuthenticationResult logoutUser(String accessToken, String refreshToken, String ipAddress) {
        try {
            User user = null;
            
            // Extract user from access token if valid
            if (accessToken != null && tokenService.validateToken(accessToken)) {
                user = tokenService.extractUserFromToken(accessToken);
            }

            // Blacklist tokens
            if (accessToken != null) {
                tokenService.blacklistToken(accessToken);
            }
            if (refreshToken != null) {
                tokenService.blacklistToken(refreshToken);
            }

            auditService.logAuthenticationEvent(user != null ? user.getId() : null, 
                "LOGOUT_SUCCESS", "User logged out successfully", ipAddress, null);

            return AuthenticationResult.success("Logout successful", null, null, null);

        } catch (Exception e) {
            auditService.logAuthenticationEvent(null, "LOGOUT_ERROR", 
                "Logout error: " + e.getMessage(), ipAddress, null);
            return AuthenticationResult.failure("Logout failed");
        }
    }

    /**
     * Verify email address
     * Requirements: 1.2, 1.3
     */
    public AuthenticationResult verifyEmail(String verificationToken, String ipAddress) {
        try {
            boolean verified = userService.activateUser(verificationToken);
            
            if (!verified) {
                auditService.logAuthenticationEvent(null, "EMAIL_VERIFICATION_FAILED", 
                    "Invalid or expired verification token", ipAddress, null);
                return AuthenticationResult.failure("Invalid or expired verification token");
            }

            // Find user to log the event
            Optional<User> userOpt = userService.findByEmail(""); // We need to find by token
            // Note: This is a limitation - we should enhance the service to return the user
            
            auditService.logAuthenticationEvent(null, "EMAIL_VERIFIED", 
                "Email verified successfully", ipAddress, null);

            return AuthenticationResult.success("Email verified successfully", null, null, null);

        } catch (Exception e) {
            auditService.logAuthenticationEvent(null, "EMAIL_VERIFICATION_ERROR", 
                "Email verification error: " + e.getMessage(), ipAddress, null);
            return AuthenticationResult.failure("Email verification failed");
        }
    }

    /**
     * Initiate password reset
     * Requirements: 4.1, 4.2
     */
    public AuthenticationResult initiatePasswordReset(String email, String ipAddress) {
        try {
            // Always return success to prevent email enumeration
            userService.initiatePasswordReset(email);
            
            auditService.logAuthenticationEvent(null, "PASSWORD_RESET_REQUESTED", 
                "Password reset requested for: " + email, ipAddress, null);

            return AuthenticationResult.success("If the email exists, a password reset link has been sent", null, null, null);

        } catch (Exception e) {
            auditService.logAuthenticationEvent(null, "PASSWORD_RESET_ERROR", 
                "Password reset error: " + e.getMessage(), ipAddress, null);
            return AuthenticationResult.failure("Password reset request failed");
        }
    }

    /**
     * Reset password using token
     * Requirements: 4.1, 4.2, 4.3
     */
    public AuthenticationResult resetPassword(String resetToken, String newPassword, String ipAddress) {
        try {
            if (!userService.isPasswordValid(newPassword)) {
                auditService.logAuthenticationEvent(null, "PASSWORD_RESET_FAILED", 
                    "Weak password provided", ipAddress, null);
                return AuthenticationResult.failure("Password does not meet security requirements");
            }

            boolean reset = userService.resetPassword(resetToken, newPassword);
            
            if (!reset) {
                auditService.logAuthenticationEvent(null, "PASSWORD_RESET_FAILED", 
                    "Invalid or expired reset token", ipAddress, null);
                return AuthenticationResult.failure("Invalid or expired reset token");
            }

            auditService.logAuthenticationEvent(null, "PASSWORD_RESET_SUCCESS", 
                "Password reset successfully", ipAddress, null);

            return AuthenticationResult.success("Password reset successfully", null, null, null);

        } catch (Exception e) {
            auditService.logAuthenticationEvent(null, "PASSWORD_RESET_ERROR", 
                "Password reset error: " + e.getMessage(), ipAddress, null);
            return AuthenticationResult.failure("Password reset failed");
        }
    }

    /**
     * Change password for authenticated user
     * Requirements: 4.3, 4.4
     */
    public AuthenticationResult changePassword(Long userId, String currentPassword, String newPassword, String ipAddress) {
        try {
            if (!userService.isPasswordValid(newPassword)) {
                auditService.logAuthenticationEvent(userId, "PASSWORD_CHANGE_FAILED", 
                    "Weak password provided", ipAddress, null);
                return AuthenticationResult.failure("Password does not meet security requirements");
            }

            boolean changed = userService.changePassword(userId, currentPassword, newPassword);
            
            if (!changed) {
                auditService.logAuthenticationEvent(userId, "PASSWORD_CHANGE_FAILED", 
                    "Invalid current password", ipAddress, null);
                return AuthenticationResult.failure("Current password is incorrect");
            }

            auditService.logAuthenticationEvent(userId, "PASSWORD_CHANGED", 
                "Password changed successfully", ipAddress, null);

            return AuthenticationResult.success("Password changed successfully", null, null, null);

        } catch (Exception e) {
            auditService.logAuthenticationEvent(userId, "PASSWORD_CHANGE_ERROR", 
                "Password change error: " + e.getMessage(), ipAddress, null);
            return AuthenticationResult.failure("Password change failed");
        }
    }

    /**
     * Authentication result wrapper
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final String message;
        private final String accessToken;
        private final String refreshToken;
        private final User user;
        private final Map<String, Object> metadata;

        private AuthenticationResult(boolean success, String message, String accessToken, String refreshToken, User user) {
            this.success = success;
            this.message = message;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
            this.metadata = new HashMap<>();
        }

        public static AuthenticationResult success(String message, String accessToken, String refreshToken, User user) {
            return new AuthenticationResult(true, message, accessToken, refreshToken, user);
        }

        public static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, message, null, null, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public User getUser() { return user; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        public AuthenticationResult withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
    }
}