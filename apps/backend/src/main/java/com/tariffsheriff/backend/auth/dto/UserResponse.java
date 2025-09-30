package com.tariffsheriff.backend.auth.dto;

import lombok.Data;

@Data
public class UserResponse {
    
    private Long id;
    private String name;
    private String email;
    private String aboutMe;
    private String role;
    private Boolean isAdmin;
    
    public UserResponse(Long id, String name, String email, String aboutMe, String role, Boolean isAdmin) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.aboutMe = aboutMe;
        this.role = role;
        this.isAdmin = isAdmin;
    }
}