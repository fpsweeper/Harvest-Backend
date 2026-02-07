package com.fpsweeper.harvest.auth.exceptions;

public class InvalidCodeException extends RuntimeException {
    public InvalidCodeException() {
        super("Invalid code");
    }
}
