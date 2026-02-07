package com.fpsweeper.harvest.auth.dto;

public class ResetPasswordRequest {
    private String code;
    private String newPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String code, String newPassword) {
        this.code = code;
        this.newPassword = newPassword;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}