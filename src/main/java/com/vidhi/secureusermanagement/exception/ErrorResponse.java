package com.vidhi.secureusermanagement.exception;

import java.time.Instant;

public class ErrorResponse {

    private int status;
    private String message;
    private String path;
    private Instant timestamp;

    public ErrorResponse(
            int status,
            String message,
            String path
    ) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = Instant.now();
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}