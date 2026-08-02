package com.diasmart.springapi.shared.security;

import com.diasmart.springapi.devices.config.DeviceActivationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestIpResolverTest {

    @Test
    void resolveShouldUseRemoteAddressByDefault() {
        DeviceActivationProperties properties = new DeviceActivationProperties();
        RequestIpResolver resolver = new RequestIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        assertEquals("10.0.0.5", resolver.resolve(request));
    }

    @Test
    void resolveShouldUseFirstForwardedAddressWhenTrusted() {
        DeviceActivationProperties properties = new DeviceActivationProperties();
        properties.setTrustForwardedHeaders(true);
        RequestIpResolver resolver = new RequestIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.1");

        assertEquals("203.0.113.10", resolver.resolve(request));
    }

    @Test
    void resolveShouldHandleUnavailableRequest() {
        DeviceActivationProperties properties = new DeviceActivationProperties();
        RequestIpResolver resolver = new RequestIpResolver(properties);

        assertNull(resolver.resolve(null));
    }

    @Test
    void resolveShouldStripIpv4Port() {
        DeviceActivationProperties properties = new DeviceActivationProperties();
        properties.setTrustForwardedHeaders(true);
        RequestIpResolver resolver = new RequestIpResolver(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10:5443");

        assertEquals("203.0.113.10", resolver.resolve(request));
    }
}
