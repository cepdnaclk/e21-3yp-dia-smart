package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiGatewayRequestTooLargeException extends ApiException {
    public AiGatewayRequestTooLargeException(String message) {
        super(HttpStatus.BAD_GATEWAY, "AI_GATEWAY_REQUEST_TOO_LARGE", message);
    }
}
