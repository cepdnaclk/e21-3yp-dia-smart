package com.diasmart.springapi.shared.dto;

import java.time.Instant;

/**
 * ErrorResponse is the standard error response wrapper for Dia-Smart REST APIs.
 *
 * Example:
 * {
 * "success": false,
 * "message": "Unauthorized access",
 * "errorCode": "ACCESS_DENIED",
 * "timestamp": "2026-05-09T00:00:00Z"
 * }
 */
public class ErrorResponse {

    private boolean success;
    private String message;
    private String errorCode;
    private Instant timestamp;

    public ErrorResponse() {
        this.success = false;
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String message, String errorCode) {
        this.success = false;
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
    }

    public static ErrorResponse of(String message, String errorCode) {
        return new ErrorResponse(message, errorCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}