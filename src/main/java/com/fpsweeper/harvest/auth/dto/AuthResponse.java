package com.fpsweeper.harvest.auth.dto;

import com.fpsweeper.harvest.user.Users;

public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Users user;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String tokenType, Users user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }
}
