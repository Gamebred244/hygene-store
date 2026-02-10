package com.codeop.store.dto;

public class AuthResponse {
    private String username;
    private String role;
    private String email;

    public AuthResponse(String username, String role, String email) {
        this.username = username;
        this.role = role;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }
}
