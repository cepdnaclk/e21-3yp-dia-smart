package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiGatewayTimeoutException extends ApiException {
    public AiGatewayTimeoutException(String message) {
        super(HttpStatus.GATEWAY_TIMEOUT, "AI_GATEWAY_TIMEOUT", message);
    }
}
