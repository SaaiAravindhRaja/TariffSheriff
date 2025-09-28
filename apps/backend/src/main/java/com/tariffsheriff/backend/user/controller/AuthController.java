package com.tariffsheriff.backend.user.controller;

import com.tariffsheriff.backend.user.dto.*;
import com.tariffsheriff.backend.user.service.AuthenticationService;
import com.tariffsheriff.backend.user.service.UserService;
import com.tariffsheriff.backend.security.jwt.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller
 * Handles all authentication-related endpoints
 * Requirements: 1.1, 2.1, 4.1, 11.4
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"${app.cors.allowed-origins:http://localhost:3000}"})
public class AuthController {
    
    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final TokenService tokenService;
    private final UserMapper userMapper;
    
    @Autowired
    public AuthController(AuthenticationService authenticationService, 
                         UserService userService,
                         TokenService tokenService,
                         UserMapper userMapper) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.tokenService = tokenService;
        this.userMapper = userMapper;
    }
    
    /**
     * User registration endpoint
     * Requirement 1.1: User registration with email verification
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, 
                                    BindingResult bindingResult,
                                    HttpServletRequest httpRequest) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> 
                    errors.put(error.getField(), error.getDefaultMessage())
                );
                
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .message("Invalid input data")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .validationErrors(errors)
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Check password confirmation
            if (!request.isPasswordConfirmed()) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("PASSWORD_MISMATCH")
                    .message("Password and confirmation password do not match")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Register user
            UserDto userDto = authenticationService.registerUser(request);
            
            ApiResponse<UserDto> response = ApiResponse.success(
                "Registration successful. Please check your email to verify your account.", 
                userDto
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("REGISTRATION_ERROR")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred during registration")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * User login endpoint
     * Requirement 2.1: Secure user authentication
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                 BindingResult bindingResult,
                                 HttpServletRequest httpRequest) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> 
                    errors.put(error.getField(), error.getDefaultMessage())
                );
                
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .message("Invalid input data")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .validationErrors(errors)
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Get client IP for audit logging
            String clientIp = getClientIpAddress(httpRequest);
            
            // Authenticate user
            AuthResponse authResponse = authenticationService.authenticateUser(request, clientIp);
            
            return ResponseEntity.ok(authResponse);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("AUTHENTICATION_FAILED")
                .message(e.getMessage())
                .status(401)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred during authentication")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * User logout endpoint
     * Requirement 2.1: Secure logout with token invalidation
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authenticationService.logoutUser(token);
            }
            
            ApiResponse<Void> response = ApiResponse.success("Logout successful");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("LOGOUT_ERROR")
                .message("An error occurred during logout")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Token refresh endpoint
     * Requirement 2.1: Token refresh functionality
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                        BindingResult bindingResult,
                                        HttpServletRequest httpRequest) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> 
                    errors.put(error.getField(), error.getDefaultMessage())
                );
                
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .message("Invalid input data")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .validationErrors(errors)
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Refresh tokens
            AuthResponse authResponse = authenticationService.refreshTokens(request.getRefreshToken());
            
            return ResponseEntity.ok(authResponse);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("TOKEN_REFRESH_FAILED")
                .message(e.getMessage())
                .status(401)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred during token refresh")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Email verification endpoint
     * Requirement 1.1: Email verification functionality
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token,
                                       HttpServletRequest httpRequest) {
        try {
            UserDto userDto = authenticationService.verifyEmail(token);
            
            ApiResponse<UserDto> response = ApiResponse.success(
                "Email verification successful. Your account is now active.", 
                userDto
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("VERIFICATION_FAILED")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred during email verification")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Forgot password endpoint
     * Requirement 4.1: Password reset request
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                          BindingResult bindingResult,
                                          HttpServletRequest httpRequest) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> 
                    errors.put(error.getField(), error.getDefaultMessage())
                );
                
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .message("Invalid input data")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .validationErrors(errors)
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Initiate password reset
            authenticationService.initiatePasswordReset(request.getEmail());
            
            ApiResponse<Void> response = ApiResponse.success(
                "If an account with that email exists, a password reset link has been sent."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Always return success to prevent email enumeration
            ApiResponse<Void> response = ApiResponse.success(
                "If an account with that email exists, a password reset link has been sent."
            );
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Reset password endpoint
     * Requirement 4.1: Password reset confirmation
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                         BindingResult bindingResult,
                                         HttpServletRequest httpRequest) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> 
                    errors.put(error.getField(), error.getDefaultMessage())
                );
                
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .message("Invalid input data")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .validationErrors(errors)
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Check password confirmation
            if (!request.isPasswordConfirmed()) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("PASSWORD_MISMATCH")
                    .message("Password and confirmation password do not match")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Reset password
            authenticationService.resetPassword(request.getToken(), request.getNewPassword());
            
            ApiResponse<Void> response = ApiResponse.success(
                "Password reset successful. You can now login with your new password."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("PASSWORD_RESET_FAILED")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred during password reset")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get current user endpoint
     * Requirement 2.1: Get authenticated user information
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest httpRequest) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("UNAUTHORIZED")
                    .message("User not authenticated")
                    .status(401)
                    .path(httpRequest.getRequestURI())
                    .build();
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            String email = authentication.getName();
            UserDto userDto = userService.getUserByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            return ResponseEntity.ok(userDto);
            
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while fetching user information")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Change password endpoint for authenticated users
     * Requirement 4.1: Password change for authenticated users
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                          BindingResult bindingResult,
                                          HttpServletRequest httpRequest) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> 
                    errors.put(error.getField(), error.getDefaultMessage())
                );
                
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("VALIDATION_ERROR")
                    .message("Invalid input data")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .validationErrors(errors)
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Check password confirmation
            if (!request.isPasswordConfirmed()) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("PASSWORD_MISMATCH")
                    .message("New password and confirmation password do not match")
                    .status(400)
                    .path(httpRequest.getRequestURI())
                    .build();
                
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                    .error("UNAUTHORIZED")
                    .message("User not authenticated")
                    .status(401)
                    .path(httpRequest.getRequestURI())
                    .build();
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            String email = authentication.getName();
            authenticationService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
            
            ApiResponse<Void> response = ApiResponse.success(
                "Password changed successfully. Please login again with your new password."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("PASSWORD_CHANGE_FAILED")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred during password change")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Utility method to extract client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}