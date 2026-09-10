package com.diasmart.springapi.ai.client;

import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.gateway.*;
import com.diasmart.springapi.ai.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiGatewayClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private AiProperties aiProperties;

    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    private AiGatewayClient client;
    private AiClinicalSummaryGatewayRequest request;

    @BeforeEach
    void setUp() {
        client = new AiGatewayClient(restClient, aiProperties);
        request = new AiClinicalSummaryGatewayRequest(
                UUID.randomUUID(), "CLINICAL_SUMMARY", "v1", "ref",
                null, null, null, null, null,
                Collections.emptyList(), Collections.emptyList()
        );

        when(aiProperties.getInternalServiceToken()).thenReturn("mock-token");

        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class, Answers.RETURNS_SELF);
        requestBodySpec = mock(RestClient.RequestBodySpec.class, Answers.RETURNS_SELF);
        responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(requestBodyUriSpec).when(restClient).post();
        doReturn(requestBodySpec).when(requestBodyUriSpec).uri(anyString());
        doReturn(responseSpec).when(requestBodySpec).retrieve();
    }

    @Test
    void shouldReturnResponseOnSuccess() {
        AiClinicalSummaryGatewayResponse expectedResponse = mock(AiClinicalSummaryGatewayResponse.class);
        doReturn(expectedResponse).when(responseSpec).body(AiClinicalSummaryGatewayResponse.class);

        AiClinicalSummaryGatewayResponse actualResponse = client.requestClinicalSummary(request);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void shouldThrowAiConfigurationExceptionWhenTokenMissing() {
        when(aiProperties.getInternalServiceToken()).thenReturn("");
        AiConfigurationException ex = assertThrows(AiConfigurationException.class, () -> client.requestClinicalSummary(request));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("AI_CONFIGURATION_ERROR", ex.getErrorCode());
    }

    @Test
    void shouldThrowGatewayTimeoutWhenSocketTimeoutOccurs() {
        doThrow(new ResourceAccessException("Timeout", new SocketTimeoutException()))
                .when(responseSpec).body(AiClinicalSummaryGatewayResponse.class);

        AiGatewayTimeoutException ex = assertThrows(AiGatewayTimeoutException.class, () -> client.requestClinicalSummary(request));
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, ex.getStatus());
        assertEquals("AI_GATEWAY_TIMEOUT", ex.getErrorCode());
    }

    @Test
    void shouldThrowGatewayUnavailableOnUnreachableResource() {
        doThrow(new ResourceAccessException("Unreachable"))
                .when(responseSpec).body(AiClinicalSummaryGatewayResponse.class);

        AiGatewayUnavailableException ex = assertThrows(AiGatewayUnavailableException.class, () -> client.requestClinicalSummary(request));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
        assertEquals("AI_GATEWAY_UNAVAILABLE", ex.getErrorCode());
    }

    @Test
    void shouldThrowGatewayAuthenticationExceptionOn401() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null
        );
        doThrow(ex).when(responseSpec).body(AiClinicalSummaryGatewayResponse.class);

        AiGatewayAuthenticationException actualEx = assertThrows(AiGatewayAuthenticationException.class, () -> client.requestClinicalSummary(request));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, actualEx.getStatus());
        assertEquals("AI_GATEWAY_AUTHENTICATION_ERROR", actualEx.getErrorCode());
    }

    @Test
    void shouldThrowGatewayAuthenticationExceptionOn403() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, null, null
        );
        doThrow(ex).when(responseSpec).body(AiClinicalSummaryGatewayResponse.class);

        AiGatewayAuthenticationException actualEx = assertThrows(AiGatewayAuthenticationException.class, () -> client.requestClinicalSummary(request));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, actualEx.getStatus());
        assertEquals("AI_GATEWAY_AUTHENTICATION_ERROR", actualEx.getErrorCode());
    }

    @Test
    void shouldThrowGatewayRequestTooLargeOn413() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large", null, null, null
        );
        doThrow(ex).when(responseSpec).body(AiClinicalSummaryGatewayResponse.class);

        AiGatewayRequestTooLargeException actualEx = assertThrows(AiGatewayRequestTooLargeException.class, () -> client.requestClinicalSummary(request));
        assertEquals(HttpStatus.BAD_GATEWAY, actualEx.getStatus());
        assertEquals("AI_GATEWAY_REQUEST_TOO_LARGE", actualEx.getErrorCode());
    }

    @Test
    void shouldThrowGatewayRequestRejectedOn422() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", null, null, null
        );
        doThrow(ex).when(responseSpec).body(AiClinicalSummaryGatewayResponse.class);

        AiGatewayRequestRejectedException actualEx = assertThrows(AiGatewayRequestRejectedException.class, () -> client.requestClinicalSummary(request));
        assertEquals(HttpStatus.BAD_GATEWAY, actualEx.getStatus());
        assertEquals("AI_GATEWAY_REQUEST_REJECTED", actualEx.getErrorCode());
    }

    @Test
    void shouldThrowGatewayErrorExceptionOn500() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", null, null, null
        );
        doThrow(ex).when(responseSpec).body(AiClinicalSummaryGatewayResponse.class);

        AiGatewayErrorException actualEx = assertThrows(AiGatewayErrorException.class, () -> client.requestClinicalSummary(request));
        assertEquals(HttpStatus.BAD_GATEWAY, actualEx.getStatus());
        assertEquals("AI_GATEWAY_ERROR", actualEx.getErrorCode());
    }
}
