package com.fpsweeper.harvest.auth.dto;

import java.util.UUID;

public class UserMeDto {

    private UUID id;
    private String email;
    private String role;

    private String authProvider;

    public UserMeDto(UUID id, String email, String role, String authProvider) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.authProvider = authProvider;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
