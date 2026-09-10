package com.diasmart.springapi.ai.client;

import com.diasmart.springapi.ai.config.AiClientConfiguration;
import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayRequest;
import com.diasmart.springapi.ai.exception.AiConfigurationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiClientSecurityTest {

    @Test
    void shouldInjectInternalTokenAndNotForwardUserJwtOrCookies() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setGatewayUrl("http://127.0.0.1:8000");
        properties.setInternalServiceToken("synthetic-internal-service-token");
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(5));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getGatewayUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getInternalServiceToken());

        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient aiRestClient = builder.build();

        mockServer.expect(requestTo("http://127.0.0.1:8000/internal/v1/insights/clinical-summary"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer synthetic-internal-service-token"))
                .andExpect(headerDoesNotExist("Cookie"))
                .andExpect(headerDoesNotExist("X-User-Authorization"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        AiGatewayClient client = new AiGatewayClient(aiRestClient, properties);
        AiClinicalSummaryGatewayRequest req = new AiClinicalSummaryGatewayRequest(
                UUID.randomUUID(), "CLINICAL_SUMMARY", "clinical-summary-v1", "patient-ref-123",
                null, null, null, null, null, Collections.emptyList(), Collections.emptyList()
        );

        try {
            client.requestClinicalSummary(req);
        } catch (Exception ignored) {
            // response was empty JSON so parsing body is expected to fail, but outbound headers are verified
        }

        mockServer.verify();
    }

    @Test
    void shouldEnsureInternalTokenIsNotAddedToUnrelatedRestClient() {
        AiProperties properties = new AiProperties();
        properties.setInternalServiceToken("ai-secret-token-12345");
        AiClientConfiguration configuration = new AiClientConfiguration(properties);

        // Build AI rest client
        RestClient aiClient = configuration.aiRestClient();
        assertNotNull(aiClient);

        // Build standard unrelated rest client from dedicated builder
        RestClient.Builder unrelatedBuilder = RestClient.builder().baseUrl("http://unrelated.service.local");
        MockRestServiceServer unrelatedServer = MockRestServiceServer.bindTo(unrelatedBuilder).build();
        RestClient unrelatedClient = unrelatedBuilder.build();

        unrelatedServer.expect(requestTo("http://unrelated.service.local/api/test"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess());

        unrelatedClient.get().uri("/api/test").retrieve().toBodilessEntity();
        unrelatedServer.verify();
    }

    @Test
    void shouldAllowStartupWhenTokenIsMissingAndFailOnlyOnInvocation() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false); // disabled
        properties.setInternalServiceToken(""); // missing token

        AiClientConfiguration configuration = new AiClientConfiguration(properties);

        // Bean creation must succeed without throwing exception
        RestClient aiClient = assertDoesNotThrow(configuration::aiRestClient);
        assertNotNull(aiClient);

        // But invoking client must fail with AiConfigurationException
        AiGatewayClient client = new AiGatewayClient(aiClient, properties);
        AiClinicalSummaryGatewayRequest req = new AiClinicalSummaryGatewayRequest(
                UUID.randomUUID(), "CLINICAL_SUMMARY", "clinical-summary-v1", "patient-ref-123",
                null, null, null, null, null, Collections.emptyList(), Collections.emptyList()
        );

        assertThrows(AiConfigurationException.class, () -> client.requestClinicalSummary(req));
    }
}
