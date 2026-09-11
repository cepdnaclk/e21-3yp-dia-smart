package com.diasmart.springapi.ai.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiErrorMappingTest {

    @Test
    void shouldVerifyAllPart3ErrorMappings() {
        // AI_DISABLED → 503
        AiDisabledException disabled = new AiDisabledException();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, disabled.getStatus());
        assertEquals("AI_DISABLED", disabled.getErrorCode());

        // AI_CONFIGURATION_ERROR → 503
        AiConfigurationException config = new AiConfigurationException("Configuration incomplete");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, config.getStatus());
        assertEquals("AI_CONFIGURATION_ERROR", config.getErrorCode());

        // AI_INSUFFICIENT_DATA → 422
        AiInsufficientDataException data = new AiInsufficientDataException();
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, data.getStatus());
        assertEquals("AI_INSUFFICIENT_DATA", data.getErrorCode());

        // AI_GATEWAY_AUTHENTICATION_ERROR → 503
        AiGatewayAuthenticationException auth = new AiGatewayAuthenticationException("Authentication failed");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, auth.getStatus());
        assertEquals("AI_GATEWAY_AUTHENTICATION_ERROR", auth.getErrorCode());

        // AI_GATEWAY_REQUEST_TOO_LARGE → 502
        AiGatewayRequestTooLargeException tooLarge = new AiGatewayRequestTooLargeException("Payload too large");
        assertEquals(HttpStatus.BAD_GATEWAY, tooLarge.getStatus());
        assertEquals("AI_GATEWAY_REQUEST_TOO_LARGE", tooLarge.getErrorCode());

        // AI_GATEWAY_REQUEST_REJECTED → 502
        AiGatewayRequestRejectedException rejected = new AiGatewayRequestRejectedException("Request rejected");
        assertEquals(HttpStatus.BAD_GATEWAY, rejected.getStatus());
        assertEquals("AI_GATEWAY_REQUEST_REJECTED", rejected.getErrorCode());

        // AI_GATEWAY_UNAVAILABLE → 503
        AiGatewayUnavailableException unavailable = new AiGatewayUnavailableException("Gateway unavailable");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatus());
        assertEquals("AI_GATEWAY_UNAVAILABLE", unavailable.getErrorCode());

        // AI_GATEWAY_TIMEOUT → 504
        AiGatewayTimeoutException timeout = new AiGatewayTimeoutException("Gateway timeout");
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, timeout.getStatus());
        assertEquals("AI_GATEWAY_TIMEOUT", timeout.getErrorCode());

        // AI_GATEWAY_ERROR → 502
        AiGatewayErrorException error = new AiGatewayErrorException("Internal gateway error");
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
        assertEquals("AI_GATEWAY_ERROR", error.getErrorCode());

        // AI_INVALID_RESPONSE → 502
        AiInvalidResponseException invalidResp = new AiInvalidResponseException("Invalid microservice response");
        assertEquals(HttpStatus.BAD_GATEWAY, invalidResp.getStatus());
        assertEquals("AI_INVALID_RESPONSE", invalidResp.getErrorCode());
    }

    @Test
    void shouldEnsureExceptionMessagesDoNotExposeInternalTokensOrUrls() {
        String safeMessage = "AI Gateway is currently unavailable.";
        AiGatewayUnavailableException ex = new AiGatewayUnavailableException(safeMessage);

        assertFalse(ex.getMessage().contains("http"));
        assertFalse(ex.getMessage().contains("8000"));
        assertFalse(ex.getMessage().contains("Bearer"));
        assertFalse(ex.getMessage().contains("token"));
    }
}
