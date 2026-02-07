package com.fpsweeper.harvest.auth.exceptions;

public class PasswordResetCodeNotFoundException extends RuntimeException {
    public PasswordResetCodeNotFoundException() {
        super("Invalid or expired reset code");
    }
}