package com.diasmart.springapi.ai.exception;

import com.diasmart.springapi.common.exceptions.ApiException;
import org.springframework.http.HttpStatus;

public class AiInsufficientDataException extends ApiException {
    public AiInsufficientDataException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "AI_INSUFFICIENT_DATA", "There is insufficient patient information for an AI-assisted summary in the selected period.");
    }
}
