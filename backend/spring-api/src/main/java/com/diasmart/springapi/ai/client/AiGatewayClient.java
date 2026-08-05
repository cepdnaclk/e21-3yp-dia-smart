package com.diasmart.springapi.ai.client;

import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayRequest;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayResponse;
import com.diasmart.springapi.ai.dto.gateway.AiGatewayErrorResponse;
import com.diasmart.springapi.ai.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

@Component
public class AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayClient.class);

    private final RestClient restClient;
    private final AiProperties aiProperties;

    public AiGatewayClient(
            @Qualifier("aiRestClient") RestClient restClient,
            AiProperties aiProperties
    ) {
        this.restClient = restClient;
        this.aiProperties = aiProperties;
    }

    public AiClinicalSummaryGatewayResponse requestClinicalSummary(AiClinicalSummaryGatewayRequest request) {
        String token = aiProperties.getInternalServiceToken();
        if (token == null || token.isBlank()) {
            throw new AiConfigurationException("AI Gateway client token is not configured.");
        }

        try {
            return restClient.post()
                    .uri("/internal/v1/insights/clinical-summary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiClinicalSummaryGatewayResponse.class);
        } catch (ResourceAccessException e) {
            log.error("Resource access error during AI Gateway summary request: {}", e.getMessage());
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new AiGatewayTimeoutException("AI Gateway read or connection timeout occurred.");
            }
            throw new AiGatewayUnavailableException("AI Gateway is unreachable: " + e.getMessage());
        } catch (HttpClientErrorException e) {
            log.error("Client error from AI Gateway: Code={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new AiGatewayAuthenticationException("AI Gateway rejected request credentials (401/403).");
            }
            String msg = "AI Gateway rejected request: " + e.getMessage();
            try {
                AiGatewayErrorResponse err = e.getResponseBodyAs(AiGatewayErrorResponse.class);
                if (err != null && err.message() != null) {
                    msg = err.message();
                }
            } catch (Exception ex) {
                // Ignore parse failures
            }
            throw new AiGatewayRequestRejectedException(msg);
        } catch (HttpServerErrorException e) {
            log.error("Server error from AI Gateway: Code={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiGatewayErrorException("AI Gateway encountered an internal error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error communicating with AI Gateway: {}", e.getMessage());
            throw new AiGatewayErrorException("Unexpected AI Gateway communication failure: " + e.getMessage());
        }
    }
}
