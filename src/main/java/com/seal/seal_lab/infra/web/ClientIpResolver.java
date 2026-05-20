package com.seal.seal_lab.infra.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String[] CANDIDATE_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    public String resolve(HttpServletRequest request) {
        for (String header : CANDIDATE_HEADERS) {
            String resolved = extractIp(request.getHeader(header));
            if (resolved != null) {
                return resolved;
            }
        }

        return request.getRemoteAddr();
    }

    private String extractIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        for (String candidate : headerValue.split(",")) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty() && !"unknown".equalsIgnoreCase(trimmed)) {
                return trimmed;
            }
        }

        return null;
    }
}
