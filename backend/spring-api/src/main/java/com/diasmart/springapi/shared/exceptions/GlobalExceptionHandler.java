package com.diasmart.springapi.shared.exceptions;

import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.shared.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
/**
 * GlobalExceptionHandler converts backend exceptions into standard API error
 * responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handles business validation errors such as duplicate email
         * or blocked public admin registration.
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
                        IllegalArgumentException exception) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.of(exception.getMessage(), "BAD_REQUEST"));
        }

        /**
         * Handles invalid login credentials.
         *
         * This is used when AuthService catches Spring Security authentication
         * failures and converts them into InvalidCredentialsException.
         */
        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(
                        InvalidCredentialsException exception) {
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of(exception.getMessage(), "UNAUTHORIZED"));
        }

        /**
         * Handles DTO validation errors such as invalid email,
         * missing password, or short password.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException exception) {
                String message = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .orElse("Validation failed");

                return ResponseEntity
                                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ErrorResponse.of(message, "VALIDATION_ERROR"));
        }

        /**
         * Handles general authentication failures.
         */
        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(
                        AuthenticationException exception) {
                return ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .body(ErrorResponse.of("Invalid email or password", "INVALID_CREDENTIALS"));
        }

        /**
         * Handles unexpected errors without exposing internal technical details.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception exception) {
                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.of("Internal server error", "INTERNAL_ERROR"));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDeniedException(
                        AccessDeniedException exception) {
                return ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(ErrorResponse.of(exception.getMessage(), "FORBIDDEN"));
        }

        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ErrorResponse> handleApiException(
                        ApiException exception) {
                return ResponseEntity
                                .status(exception.getStatus())
                                .body(ErrorResponse.of(exception.getMessage(), exception.getErrorCode()));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
                        HttpMessageNotReadableException exception) {
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.of("Request body is missing or malformed", "BAD_REQUEST"));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse>
        handleResourceNotFoundException(
                ResourceNotFoundException exception
        ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        ErrorResponse.of(
                                exception.getMessage(),
                                "NOT_FOUND"
                        )
                );
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse>
        handleDataIntegrityViolationException(
                DataIntegrityViolationException exception
        ) {

        String message =
                "Database constraint violation";

        String exceptionMessage =
                exception.getMostSpecificCause()
                        .getMessage();

        if (exceptionMessage.contains(
                "dose_schedules_prescription_id_schedule_label_key"
        )) {

                message =
                        "Schedule label already exists for this prescription";

        } else if (exceptionMessage.contains(
                "prescriptions_insulin_product_id_fkey"
        )) {

                message =
                        "Referenced insulin product does not exist";

        } else if (exceptionMessage.contains(
                "dose_schedules_prescription_id_fkey"
        )) {

                message =
                        "Referenced prescription does not exist";
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse.of(
                                message,
                                "BAD_REQUEST"
                        )
                );
        }
}