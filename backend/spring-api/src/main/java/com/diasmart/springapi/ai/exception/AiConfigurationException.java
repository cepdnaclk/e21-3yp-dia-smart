package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiConfigurationException extends ApiException {
    public AiConfigurationException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "AI_CONFIGURATION_ERROR", message);
    }
}
