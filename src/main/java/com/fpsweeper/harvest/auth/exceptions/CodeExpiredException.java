package com.fpsweeper.harvest.auth.exceptions;

public class CodeExpiredException extends RuntimeException {
    public CodeExpiredException() {
        super("Code expired");
    }
}
