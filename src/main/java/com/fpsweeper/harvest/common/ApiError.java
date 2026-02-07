package com.fpsweeper.harvest.common;

import java.time.Instant;

public class ApiError {

    private int status;
    private String message;
    private Instant timestamp = Instant.now();

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
