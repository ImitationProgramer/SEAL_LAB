package com.seal.seal_lab.infra.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PasswordSessionService {

    private static final String AUTH_ESTABLISHED_AT = "security.authEstablishedAt";

    public void markAuthenticationEstablished(HttpSession session) {
        session.setAttribute(AUTH_ESTABLISHED_AT, LocalDateTime.now());
    }

    public LocalDateTime getAuthenticationEstablishedAt(HttpSession session) {
        Object value = session.getAttribute(AUTH_ESTABLISHED_AT);
        return value instanceof LocalDateTime dateTime ? dateTime : null;
    }

    public boolean isSessionStale(HttpSession session, LocalDateTime passwordChangedAt) {
        if (passwordChangedAt == null) {
            return false;
        }
        LocalDateTime authEstablishedAt = getAuthenticationEstablishedAt(session);
        if (authEstablishedAt == null) {
            return false;
        }
        return authEstablishedAt.isBefore(passwordChangedAt);
    }

    public void clear(HttpSession session) {
        session.removeAttribute(AUTH_ESTABLISHED_AT);
    }
}
