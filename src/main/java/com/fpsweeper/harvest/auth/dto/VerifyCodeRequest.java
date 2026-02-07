package com.fpsweeper.harvest.auth.dto;
public class VerifyCodeRequest {

    private String email;
    private String code;

    public VerifyCodeRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
