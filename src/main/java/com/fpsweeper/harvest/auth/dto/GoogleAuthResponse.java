package com.fpsweeper.harvest.auth.dto;

public class GoogleAuthResponse {
    private String accessToken;
    private UserDto user;

    public GoogleAuthResponse(String accessToken, UserDto user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    // Getters and setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}
