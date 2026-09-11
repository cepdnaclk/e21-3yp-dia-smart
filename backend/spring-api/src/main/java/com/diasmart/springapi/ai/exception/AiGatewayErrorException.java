package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiGatewayErrorException extends ApiException {
    public AiGatewayErrorException(String message) {
        super(HttpStatus.BAD_GATEWAY, "AI_GATEWAY_ERROR", message);
    }
}
