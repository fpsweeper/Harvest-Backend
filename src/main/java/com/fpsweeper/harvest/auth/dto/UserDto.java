package com.fpsweeper.harvest.auth.dto;

public class UserDto {
    private String email;
    private String role;
    private String authProvider;

    public UserDto(String email, String role, String authProvider) {
        this.email = email;
        this.role = role;
        this.authProvider = authProvider;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }
}