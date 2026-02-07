package com.fpsweeper.harvest.auth.exceptions;

public class GoogleSigninException extends RuntimeException {
    public GoogleSigninException() {
        super("This account uses Google Sign-In. Please login with Google.");
    }
}
