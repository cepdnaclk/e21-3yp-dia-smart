package com.diasmart.springapi.shared.exceptions;

/**
 * InvalidCredentialsException is thrown when login credentials are invalid.
 *
 * We use a custom exception instead of exposing Spring Security's internal
 * BadCredentialsException directly to the API response layer.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}