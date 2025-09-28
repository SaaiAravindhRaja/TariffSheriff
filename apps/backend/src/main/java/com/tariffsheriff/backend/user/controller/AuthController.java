package com.tariffsheriff.backend.user.controller;

import com.tariffsheriff.backend.user.dto.*;
import com.tariffsheriff.backend.user.service.AuthenticationService;
import com.tariffsheriff.backend.user.service.UserService;
import com.tariffsheriff.backend.security.jwt.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "User authentication and account management endpoints")
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
    @Operation(
        summary = "Register a new user account",
        description = "Creates a new user account with email verification. The user will receive an email with a verification link that must be clicked to activate the account.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Registration successful. Verification email sent.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Registration Success",
                    value = """
                    {
                      "success": true,
                      "message": "Registration successful. Please check your email to verify your account.",
                      "data": {
                        "id": 1,
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "role": "USER",
                        "status": "PENDING",
                        "emailVerified": false,
                        "createdAt": "2024-01-15T10:30:00"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or email already exists",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Validation Error",
                        value = """
                        {
                          "error": "VALIDATION_ERROR",
                          "message": "Invalid input data",
                          "status": 400,
                          "path": "/api/auth/register",
                          "timestamp": "2024-01-15T10:30:00",
                          "validation_errors": {
                            "email": "Email must be valid",
                            "password": "Password must contain at least one uppercase letter"
                          }
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Email Already Exists",
                        value = """
                        {
                          "error": "REGISTRATION_ERROR",
                          "message": "Email address is already registered",
                          "status": 400,
                          "path": "/api/auth/register",
                          "timestamp": "2024-01-15T10:30:00"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Internal Error",
                    value = """
                    {
                      "error": "INTERNAL_ERROR",
                      "message": "An unexpected error occurred during registration",
                      "status": 500,
                      "path": "/api/auth/register",
                      "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
        @Parameter(description = "User registration details", required = true)
        @Valid @RequestBody RegisterRequest request, 
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
            
            com.tariffsheriff.backend.user.dto.ApiResponse<UserDto> response = com.tariffsheriff.backend.user.dto.ApiResponse.success(
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
    @Operation(
        summary = "Authenticate user and obtain access tokens",
        description = "Authenticates a user with email and password, returning JWT access and refresh tokens. The access token expires in 15 minutes, while the refresh token expires in 7 days.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Login Success",
                    value = """
                    {
                      "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "token_type": "Bearer",
                      "expires_in": 900,
                      "user": {
                        "id": 1,
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "role": "USER",
                        "status": "ACTIVE",
                        "emailVerified": true,
                        "lastLogin": "2024-01-15T10:30:00"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Validation Error",
                    value = """
                    {
                      "error": "VALIDATION_ERROR",
                      "message": "Invalid input data",
                      "status": 400,
                      "path": "/api/auth/login",
                      "timestamp": "2024-01-15T10:30:00",
                      "validation_errors": {
                        "email": "Email must be valid",
                        "password": "Password is required"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid Credentials",
                        value = """
                        {
                          "error": "AUTHENTICATION_FAILED",
                          "message": "Invalid email or password",
                          "status": 401,
                          "path": "/api/auth/login",
                          "timestamp": "2024-01-15T10:30:00"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Account Locked",
                        value = """
                        {
                          "error": "AUTHENTICATION_FAILED",
                          "message": "Account is locked due to multiple failed login attempts",
                          "status": 401,
                          "path": "/api/auth/login",
                          "timestamp": "2024-01-15T10:30:00"
                        }
                        """
                    )
                }
            )
        )
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
        @Parameter(description = "User login credentials", required = true)
        @Valid @RequestBody LoginRequest request,
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
    @Operation(
        summary = "Logout user and invalidate tokens",
        description = "Logs out the current user by invalidating their access token and adding it to the blacklist. The refresh token is also invalidated.",
        tags = {"Authentication"},
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Logout successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Logout Success",
                    value = """
                    {
                      "success": true,
                      "message": "Logout successful"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        try {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authenticationService.logoutUser(token);
            }
            
            com.tariffsheriff.backend.user.dto.ApiResponse<Void> response = com.tariffsheriff.backend.user.dto.ApiResponse.success("Logout successful");
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
    @Operation(
        summary = "Refresh access token using refresh token",
        description = "Generates new access and refresh tokens using a valid refresh token. The old refresh token is invalidated and a new one is issued.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token refresh successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AuthResponse.class),
                examples = @ExampleObject(
                    name = "Refresh Success",
                    value = """
                    {
                      "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "token_type": "Bearer",
                      "expires_in": 900,
                      "user": {
                        "id": 1,
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "role": "USER",
                        "status": "ACTIVE"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid or expired refresh token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Invalid Refresh Token",
                    value = """
                    {
                      "error": "TOKEN_REFRESH_FAILED",
                      "message": "Invalid or expired refresh token",
                      "status": 401,
                      "path": "/api/auth/refresh",
                      "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
        @Parameter(description = "Refresh token request", required = true)
        @Valid @RequestBody RefreshTokenRequest request,
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
    @Operation(
        summary = "Verify user email address",
        description = "Verifies a user's email address using the verification token sent via email during registration. Once verified, the user account becomes active.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email verification successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Verification Success",
                    value = """
                    {
                      "success": true,
                      "message": "Email verification successful. Your account is now active.",
                      "data": {
                        "id": 1,
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "role": "USER",
                        "status": "ACTIVE",
                        "emailVerified": true
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid or expired verification token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Invalid Token",
                    value = """
                    {
                      "error": "VERIFICATION_FAILED",
                      "message": "Invalid or expired verification token",
                      "status": 400,
                      "path": "/api/auth/verify",
                      "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(
        @Parameter(description = "Email verification token", required = true, example = "abc123def456")
        @RequestParam("token") String token,
        HttpServletRequest httpRequest) {
        try {
            UserDto userDto = authenticationService.verifyEmail(token);
            
            com.tariffsheriff.backend.user.dto.ApiResponse<UserDto> response = com.tariffsheriff.backend.user.dto.ApiResponse.success(
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
    @Operation(
        summary = "Request password reset",
        description = "Initiates a password reset process by sending a secure reset link to the user's email address. For security reasons, this endpoint always returns success regardless of whether the email exists.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password reset email sent (if account exists)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Reset Email Sent",
                    value = """
                    {
                      "success": true,
                      "message": "If an account with that email exists, a password reset link has been sent."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
        @Parameter(description = "Password reset request with email", required = true)
        @Valid @RequestBody ForgotPasswordRequest request,
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
            
            com.tariffsheriff.backend.user.dto.ApiResponse<Void> response = com.tariffsheriff.backend.user.dto.ApiResponse.success(
                "If an account with that email exists, a password reset link has been sent."
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Always return success to prevent email enumeration
            com.tariffsheriff.backend.user.dto.ApiResponse<Void> response = com.tariffsheriff.backend.user.dto.ApiResponse.success(
                "If an account with that email exists, a password reset link has been sent."
            );
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Reset password endpoint
     * Requirement 4.1: Password reset confirmation
     */
    @Operation(
        summary = "Reset password using reset token",
        description = "Resets a user's password using the reset token received via email. The token is valid for 1 hour and can only be used once.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password reset successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Reset Success",
                    value = """
                    {
                      "success": true,
                      "message": "Password reset successful. You can now login with your new password."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or expired token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Invalid Token",
                        value = """
                        {
                          "error": "PASSWORD_RESET_FAILED",
                          "message": "Invalid or expired reset token",
                          "status": 400,
                          "path": "/api/auth/reset-password",
                          "timestamp": "2024-01-15T10:30:00"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Password Mismatch",
                        value = """
                        {
                          "error": "PASSWORD_MISMATCH",
                          "message": "Password and confirmation password do not match",
                          "status": 400,
                          "path": "/api/auth/reset-password",
                          "timestamp": "2024-01-15T10:30:00"
                        }
                        """
                    )
                }
            )
        )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
        @Parameter(description = "Password reset confirmation with new password", required = true)
        @Valid @RequestBody ResetPasswordRequest request,
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
            
            com.tariffsheriff.backend.user.dto.ApiResponse<Void> response = com.tariffsheriff.backend.user.dto.ApiResponse.success(
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
    @Operation(
        summary = "Get current authenticated user information",
        description = "Retrieves the profile information of the currently authenticated user based on the JWT token.",
        tags = {"Authentication"},
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User information retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "Current User",
                    value = """
                    {
                      "id": 1,
                      "name": "John Doe",
                      "email": "john.doe@example.com",
                      "aboutMe": "Software developer with 5 years of experience",
                      "role": "USER",
                      "status": "ACTIVE",
                      "emailVerified": true,
                      "lastLogin": "2024-01-15T10:30:00",
                      "createdAt": "2024-01-01T09:00:00"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                    name = "Unauthorized",
                    value = """
                    {
                      "error": "UNAUTHORIZED",
                      "message": "User not authenticated",
                      "status": 401,
                      "path": "/api/auth/me",
                      "timestamp": "2024-01-15T10:30:00"
                    }
                    """
                )
            )
        )
    })
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
    @Operation(
        summary = "Change password for authenticated user",
        description = "Allows an authenticated user to change their password by providing their current password and a new password. All existing sessions will be invalidated after a successful password change.",
        tags = {"Authentication"},
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password changed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Password Change Success",
                    value = """
                    {
                      "success": true,
                      "message": "Password changed successfully. Please login again with your new password."
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data or incorrect current password",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Incorrect Current Password",
                        value = """
                        {
                          "error": "PASSWORD_CHANGE_FAILED",
                          "message": "Current password is incorrect",
                          "status": 400,
                          "path": "/api/auth/change-password",
                          "timestamp": "2024-01-15T10:30:00"
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Password Mismatch",
                        value = """
                        {
                          "error": "PASSWORD_MISMATCH",
                          "message": "New password and confirmation password do not match",
                          "status": 400,
                          "path": "/api/auth/change-password",
                          "timestamp": "2024-01-15T10:30:00"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
        @Parameter(description = "Password change request with current and new passwords", required = true)
        @Valid @RequestBody ChangePasswordRequest request,
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
            
            com.tariffsheriff.backend.user.dto.ApiResponse<Void> response = com.tariffsheriff.backend.user.dto.ApiResponse.success(
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