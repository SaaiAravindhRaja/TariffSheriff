package com.tariffsheriff.backend.user.dto;

import com.tariffsheriff.backend.user.model.User;
import org.springframework.stereotype.Component;

/**
 * Mapper utility for converting between User entities and DTOs
 * Requirement 2.3: Safe data transfer without exposing sensitive information
 */
@Component
public class UserMapper {
    
    /**
     * Converts User entity to UserDto
     * Excludes sensitive information like password and tokens
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        
        return new UserDto(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getAboutMe(),
            user.getRole(),
            user.getStatus(),
            user.isEmailVerified(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
    
    /**
     * Updates User entity from RegisterRequest
     * Used during user registration
     */
    public void updateUserFromRegisterRequest(User user, RegisterRequest request) {
        if (user == null || request == null) {
            return;
        }
        
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setAboutMe(request.getAboutMe());
        
        // Set role if provided, default to USER
        if (request.getRole() != null) {
            try {
                user.setRole(com.tariffsheriff.backend.user.model.UserRole.valueOf(request.getRole()));
            } catch (IllegalArgumentException e) {
                user.setRole(com.tariffsheriff.backend.user.model.UserRole.USER);
            }
        } else {
            user.setRole(com.tariffsheriff.backend.user.model.UserRole.USER);
        }
    }
}