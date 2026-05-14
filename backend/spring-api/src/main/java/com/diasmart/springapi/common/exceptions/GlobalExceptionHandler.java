package com.diasmart.springapi.common.exceptions;

import com.diasmart.springapi.common.responses.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException ex
    ) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(
                        ApiResponse.failure(
                                ex.getMessage(),
                                ex.getErrorCode()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        String message =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + " "
                                        + error.getDefaultMessage()
                        )
                        .orElse("Invalid request");

        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.failure(
                                message,
                                "VALIDATION_ERROR"
                        )
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .badRequest()
                .body(
                        ApiResponse.failure(
                                ex.getMessage(),
                                "INVALID_REQUEST"
                        )
                );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        ApiResponse.failure(
                                "Request conflicts with existing database data",
                                "DATA_INTEGRITY_VIOLATION"
                        )
                );
    }
}
