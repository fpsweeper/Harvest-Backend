package com.fpsweeper.harvest.auth.exceptions;

// Change from Exception to RuntimeException
public class DepositException extends RuntimeException {
    public DepositException(String message) {
        super(message);
    }
}