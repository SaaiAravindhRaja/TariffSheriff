package com.tariffsheriff.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    
    private String token;
    private String type = "Bearer";
    private Long id;
    private String name;
    private String email;
    private String role;
    private Boolean isAdmin;
    
    public AuthResponse(String token, Long id, String name, String email, String role, Boolean isAdmin) {
        this.token = token;
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isAdmin = isAdmin;
    }
}