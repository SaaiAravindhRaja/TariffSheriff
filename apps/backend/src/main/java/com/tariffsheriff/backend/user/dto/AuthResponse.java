package com.tariffsheriff.backend.user.dto;

public class AuthResponse {

       private String token;
    private String username;
    private String authority;

    public AuthResponse(String token, String username, String authority) {
        this.token = token;
        this.username = username;
        this.authority = authority;
    }
        public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthority() {
        return authority;
    }
}
