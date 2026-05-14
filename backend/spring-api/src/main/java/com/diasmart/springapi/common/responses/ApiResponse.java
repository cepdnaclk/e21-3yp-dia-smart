package com.diasmart.springapi.common.responses;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    private OffsetDateTime timestamp;

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        return response;
    }

    public static <T> ApiResponse<T> failure(
            String message,
            String errorCode
    ) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorCode(errorCode);
        response.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        return response;
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
