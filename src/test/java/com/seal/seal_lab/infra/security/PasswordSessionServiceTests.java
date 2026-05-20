package com.seal.seal_lab.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordSessionServiceTests {

    private final PasswordSessionService passwordSessionService = new PasswordSessionService();

    @Test
    void marksAuthenticationEstablishedAndReturnsTimestamp() {
        MockHttpSession session = new MockHttpSession();

        passwordSessionService.markAuthenticationEstablished(session);

        assertThat(passwordSessionService.getAuthenticationEstablishedAt(session)).isNotNull();
    }

    @Test
    void sessionIsStaleWhenPasswordWasChangedAfterAuthentication() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("security.authEstablishedAt", LocalDateTime.now().minusMinutes(30));

        assertThat(passwordSessionService.isSessionStale(session, LocalDateTime.now().minusMinutes(5))).isTrue();
        assertThat(passwordSessionService.isSessionStale(session, LocalDateTime.now().minusHours(1))).isFalse();
    }
}
