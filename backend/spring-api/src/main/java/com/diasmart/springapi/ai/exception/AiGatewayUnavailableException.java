package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiGatewayUnavailableException extends ApiException {
    public AiGatewayUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_GATEWAY_UNAVAILABLE", message);
    }
}
