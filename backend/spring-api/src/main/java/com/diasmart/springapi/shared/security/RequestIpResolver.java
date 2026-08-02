package com.diasmart.springapi.shared.security;

import com.diasmart.springapi.devices.config.DeviceActivationProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestIpResolver {

    private final DeviceActivationProperties properties;

    public RequestIpResolver(DeviceActivationProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        if (properties.isTrustForwardedHeaders()) {
            String forwarded = firstForwardedIp(request);

            if (forwarded != null) {
                return forwarded;
            }
        }

        return normalize(request.getRemoteAddr());
    }

    private String firstForwardedIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null) {
            String first = xForwardedFor.split(",")[0];
            String normalized = normalize(first);

            if (normalized != null) {
                return normalized;
            }
        }

        String xRealIp = normalize(request.getHeader("X-Real-IP"));

        if (xRealIp != null) {
            return xRealIp;
        }

        return parseForwardedHeader(request.getHeader("Forwarded"));
    }

    private String parseForwardedHeader(String header) {
        if (header == null) {
            return null;
        }

        for (String part : header.split(";")) {
            String trimmed = part.trim();

            if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("for=")) {
                return normalize(trimmed.substring(4));
            }
        }

        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim()
                .replace("\"", "")
                .replace("[", "")
                .replace("]", "");

        if (normalized.isBlank() || "unknown".equalsIgnoreCase(normalized)) {
            return null;
        }

        if (normalized.indexOf(':') == normalized.lastIndexOf(':')
                && normalized.contains(".")) {
            int portSeparator = normalized.indexOf(':');

            if (portSeparator > -1) {
                normalized = normalized.substring(0, portSeparator);
            }
        }

        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }
}
