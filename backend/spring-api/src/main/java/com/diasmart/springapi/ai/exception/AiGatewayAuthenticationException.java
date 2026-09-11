package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiGatewayAuthenticationException extends ApiException {
    public AiGatewayAuthenticationException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_GATEWAY_AUTHENTICATION_ERROR", message);
    }
}
