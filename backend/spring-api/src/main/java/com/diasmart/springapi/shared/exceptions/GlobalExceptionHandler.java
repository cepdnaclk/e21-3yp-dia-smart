package com.diasmart.springapi.shared.exceptions;

import com.diasmart.springapi.shared.dto.ErrorResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.AuthenticationException;

import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler converts backend exceptions
 * into standard API error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles business validation errors such as
     * duplicate email or blocked admin registration.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse>
    handleIllegalArgumentException(

            IllegalArgumentException exception
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.of(
                                exception.getMessage(),
                                "BAD_REQUEST"
                        )
                );
    }


    /**
     * Handles DTO validation errors such as invalid email,
     * missing password, or short password.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
    handleValidationException(

            MethodArgumentNotValidException exception
    ) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error ->
                        error.getField()
                                + ": "
                                + error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(
                        ErrorResponse.of(
                                message,
                                "VALIDATION_ERROR"
                        )
                );
    }


    /**
     * Handles failed login attempts.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse>
    handleAuthenticationException(

            AuthenticationException exception
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        ErrorResponse.of(
                                "Invalid email or password",
                                "INVALID_CREDENTIALS"
                        )
                );
    }


    /**
     * Handles runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse>
    handleRuntimeException(

            RuntimeException exception
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.of(
                                exception.getMessage(),
                                "RUNTIME_ERROR"
                        )
                );
    }


    /**
     * Handles unexpected errors without exposing
     * internal technical details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>
    handleGenericException(

            Exception exception
    ) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.of(
                                "Internal server error",
                                "INTERNAL_ERROR"
                        )
                );
    }
}