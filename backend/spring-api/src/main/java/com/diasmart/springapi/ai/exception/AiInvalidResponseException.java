package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiInvalidResponseException extends ApiException {
    public AiInvalidResponseException(String message) {
        super(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE", message);
    }
}
