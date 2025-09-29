package com.tariffsheriff.backend.user.controller;

import com.tariffsheriff.backend.user.dto.*;
import com.tariffsheriff.backend.user.model.User;
import com.tariffsheriff.backend.user.model.UserRole;
import com.tariffsheriff.backend.user.model.UserStatus;
import com.tariffsheriff.backend.user.service.UserService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced User Management Controller
 * Handles user management operations with authentication and role-based access control
 * Requirements: 5.2, 5.3, 5.4, 7.4
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"${app.cors.allowed-origins:http://localhost:3000}"})
@Tag(name = "User Management", description = "User management and administration endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * Get all users (Admin only)
     * Requirement 5.2: Role-based access control for administrative operations
     */
    @Operation(
        summary = "Get all users with pagination",
        description = "Retrieves a paginated list of all users in the system. Only accessible by administrators.",
        tags = {"User Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Users List",
                    value = """
                    {
                      "content": [
                        {
                          "id": 1,
                          "name": "John Doe",
                          "email": "john.doe@example.com",
                          "role": "USER",
                          "status": "ACTIVE",
                          "emailVerified": true,
                          "lastLogin": "2024-01-15T10:30:00",
                          "createdAt": "2024-01-01T09:00:00"
                        }
                      ],
                      "pageable": {
                        "pageNumber": 0,
                        "pageSize": 20,
                        "sort": {
                          "sorted": true,
                          "ascending": true
                        }
                      },
                      "totalElements": 1,
                      "totalPages": 1,
                      "first": true,
                      "last": true
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin role required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest httpRequest) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<User> userPage = userService.getAllUsers(pageable);
            
            // Convert to DTOs to exclude sensitive information
            Page<UserDto> userDtoPage = userPage.map(userMapper::toDto);
            
            return ResponseEntity.ok(userDtoPage);
            
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while fetching users")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get user by ID (Admin or own profile)
     * Requirement 5.3: User profile management with security validations
     */
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves a specific user by their ID. Users can only access their own profile unless they have admin privileges.",
        tags = {"User Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(
                    name = "User Details",
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
            responseCode = "403",
            description = "Forbidden - Can only access own profile or admin required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.isCurrentUser(#id)")
    public ResponseEntity<?> getUserById(
        @Parameter(description = "User ID", required = true, example = "1")
        @PathVariable Long id, 
        HttpServletRequest httpRequest) {
        try {
            User user = userService.getUserById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            UserDto userDto = userMapper.toDto(user);
            return ResponseEntity.ok(userDto);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("USER_NOT_FOUND")
                .message(e.getMessage())
                .status(404)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while fetching user")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update user profile (Admin or own profile)
     * Requirement 5.3: User profile management with security validations
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.isCurrentUser(#id)")
    public ResponseEntity<?> updateUser(@PathVariable Long id, 
                                      @Valid @RequestBody UpdateUserRequest request,
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
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            User updatedUser = userService.updateUser(id, request, isAdmin)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            UserDto userDto = userMapper.toDto(updatedUser);
            
            com.tariffsheriff.backend.user.dto.ApiResponse<UserDto> response = com.tariffsheriff.backend.user.dto.ApiResponse.success("User updated successfully", userDto);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("UPDATE_FAILED")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while updating user")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete user (Admin only)
     * Requirement 5.2: Role-based access control for administrative operations
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            userService.deleteUser(id);
            
            com.tariffsheriff.backend.user.dto.ApiResponse<Void> response = com.tariffsheriff.backend.user.dto.ApiResponse.success("User deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("DELETE_FAILED")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while deleting user")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Lock user account (Admin only)
     * Requirement 5.2: Administrative user management operations
     */
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> lockUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            userService.lockUser(id);
            
            com.tariffsheriff.backend.user.dto.ApiResponse<Void> response = com.tariffsheriff.backend.user.dto.ApiResponse.success("User account locked successfully");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("LOCK_FAILED")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while locking user")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Unlock user account (Admin only)
     * Requirement 5.2: Administrative user management operations
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unlockUser(@PathVariable Long id, HttpServletRequest httpRequest) {
        try {
            userService.unlockUser(id);
            
            com.tariffsheriff.backend.user.dto.ApiResponse<Void> response = com.tariffsheriff.backend.user.dto.ApiResponse.success("User account unlocked successfully");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("UNLOCK_FAILED")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while unlocking user")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Change user role (Admin only)
     * Requirement 5.2: Role-based access control management
     */
    @Operation(
        summary = "Change user role",
        description = "Changes a user's role. Only accessible by administrators. Available roles: USER, ANALYST, ADMIN.",
        tags = {"User Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User role changed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Role Change Success",
                    value = """
                    {
                      "success": true,
                      "message": "User role changed successfully",
                      "data": {
                        "id": 1,
                        "name": "John Doe",
                        "email": "john.doe@example.com",
                        "role": "ANALYST",
                        "status": "ACTIVE"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role or user not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin role required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserRole(
        @Parameter(description = "User ID", required = true, example = "1")
        @PathVariable Long id, 
        @Parameter(description = "Role change request", required = true)
        @Valid @RequestBody ChangeRoleRequest request,
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
            
            User updatedUser = userService.changeUserRole(id, UserRole.valueOf(request.getRole()));
            UserDto userDto = userMapper.toDto(updatedUser);
            
            com.tariffsheriff.backend.user.dto.ApiResponse<UserDto> response = com.tariffsheriff.backend.user.dto.ApiResponse.success("User role changed successfully", userDto);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("ROLE_CHANGE_FAILED")
                .message(e.getMessage())
                .status(400)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while changing user role")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get user statistics (Admin only)
     * Requirement 5.4: Administrative reporting and analytics
     */
    @Operation(
        summary = "Get user statistics",
        description = "Retrieves comprehensive user statistics including total users, active users, and role distribution. Only accessible by administrators.",
        tags = {"User Management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User statistics retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "User Statistics",
                    value = """
                    {
                      "totalUsers": 150,
                      "activeUsers": 142,
                      "pendingUsers": 5,
                      "lockedUsers": 3,
                      "usersByRole": {
                        "USER": 120,
                        "ANALYST": 25,
                        "ADMIN": 5
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Admin role required",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStats(HttpServletRequest httpRequest) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userService.getTotalUserCount());
            stats.put("activeUsers", userService.getActiveUserCount());
            stats.put("pendingUsers", userService.getPendingUserCount());
            stats.put("lockedUsers", userService.getLockedUserCount());
            
            Map<UserRole, Long> roleStats = userService.getUserCountByRole();
            stats.put("usersByRole", roleStats);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while fetching user statistics")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Search users (Admin only)
     * Requirement 5.2: Administrative user search and filtering
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<User> userPage = userService.searchUsers(query, pageable);
            
            // Convert to DTOs to exclude sensitive information
            Page<UserDto> userDtoPage = userPage.map(userMapper::toDto);
            
            return ResponseEntity.ok(userDtoPage);
            
        } catch (Exception e) {
            ErrorResponse errorResponse = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred while searching users")
                .status(500)
                .path(httpRequest.getRequestURI())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
