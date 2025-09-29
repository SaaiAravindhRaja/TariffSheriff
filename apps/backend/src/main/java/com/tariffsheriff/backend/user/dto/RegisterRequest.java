package com.tariffsheriff.backend.user.dto;

public class RegisterRequest {
        private String name;
    private String email;
    private String password;
    private String role; // optional, default "USER"
    // getters & setters
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
    public String getRole() {
        return role;
    }
}
