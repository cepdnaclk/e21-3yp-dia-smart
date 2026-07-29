package com.diasmart.springapi.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    private final CorsConfigurationSource source = new CorsConfig().corsConfigurationSource();

    @Test
    void allowsCapacitorOriginForAuthLoginPreflight() {
        CorsConfiguration configuration = source.getCorsConfiguration(
                preflightRequest("/api/v1/auth/login"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://localhost")).isEqualTo("https://localhost");
        assertThat(configuration.checkHttpMethod(org.springframework.http.HttpMethod.POST)).isNotNull();
        assertThat(configuration.checkHeaders(List.of("content-type"))).contains("content-type");
    }

    @Test
    void allowsCapacitorOriginForAuthRegisterPreflight() {
        CorsConfiguration configuration = source.getCorsConfiguration(
                preflightRequest("/api/v1/auth/register"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://localhost")).isEqualTo("https://localhost");
        assertThat(configuration.checkHttpMethod(org.springframework.http.HttpMethod.POST)).isNotNull();
        assertThat(configuration.checkHeaders(List.of("content-type"))).contains("content-type");
    }

    @Test
    void preservesExistingWebOrigins() {
        CorsConfiguration configuration = source.getCorsConfiguration(
                preflightRequest("/api/v1/auth/login"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://diasmart.xyz")).isEqualTo("https://diasmart.xyz");
        assertThat(configuration.checkOrigin("https://www.diasmart.xyz")).isEqualTo("https://www.diasmart.xyz");
        assertThat(configuration.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
    }

    private static HttpServletRequest preflightRequest(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", requestUri);
        request.addHeader(HttpHeaders.ORIGIN, "https://localhost");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type");
        return request;
    }
}
