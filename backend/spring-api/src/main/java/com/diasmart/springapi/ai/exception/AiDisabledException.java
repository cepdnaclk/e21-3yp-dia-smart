package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiDisabledException extends ApiException {
    public AiDisabledException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_DISABLED", "AI-assisted clinical summaries are currently disabled.");
    }
}
