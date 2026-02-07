package com.fpsweeper.harvest.auth.dto;
public class GoogleAuthRequest {
    private String idToken;

    public GoogleAuthRequest() {
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}

