package com.seal.seal_lab.infra.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpResolverTests {

    private final ClientIpResolver clientIpResolver = new ClientIpResolver();

    @Test
    void prefersFirstValidForwardedIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.5");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void skipsUnknownForwardedValuesAndFallsBackToRealIpHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.24");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("198.51.100.24");
    }

    @Test
    void fallsBackToRemoteAddrWhenProxyHeadersAreMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertThat(clientIpResolver.resolve(request)).isEqualTo("127.0.0.1");
    }
}
