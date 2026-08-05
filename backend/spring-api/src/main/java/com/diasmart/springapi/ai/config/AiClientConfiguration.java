package com.diasmart.springapi.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfiguration {

    private final AiProperties aiProperties;

    public AiClientConfiguration(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Bean(name = "aiRestClient")
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        if (aiProperties.getConnectTimeout() != null) {
            factory.setConnectTimeout((int) aiProperties.getConnectTimeout().toMillis());
        }
        if (aiProperties.getReadTimeout() != null) {
            factory.setReadTimeout((int) aiProperties.getReadTimeout().toMillis());
        }

        String baseUrl = aiProperties.getGatewayUrl();
        if (baseUrl == null) {
            baseUrl = "http://127.0.0.1:8000";
        }

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl);

        String token = aiProperties.getInternalServiceToken();
        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token.trim());
        }

        return builder.build();
    }
}
