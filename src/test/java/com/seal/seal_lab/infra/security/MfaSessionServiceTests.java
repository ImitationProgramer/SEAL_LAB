package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.infra.config.MfaStepUpProperties;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MfaSessionServiceTests {

    private MfaSessionService mfaSessionService;
    private HttpSession session;

    @BeforeEach
    void setUp() {
        MfaStepUpProperties properties = new MfaStepUpProperties();
        properties.setTtlMinutes(10);
        properties.setValidateRequestIp(true);
        properties.setValidateFingerprint(true);
        mfaSessionService = new MfaSessionService(properties);
        session = new MockHttpSession();
    }

    @Test
    void loginVerificationStateIsTracked() {
        mfaSessionService.beginLoginVerification(session, "admin", true);

        assertThat(mfaSessionService.isLoginVerificationRequired(session)).isTrue();
        assertThat(mfaSessionService.isSetupRequired(session)).isTrue();
        assertThat(mfaSessionService.getLoginId(session)).isEqualTo("admin");

        mfaSessionService.markLoginVerified(session);

        assertThat(mfaSessionService.isLoginVerificationRequired(session)).isFalse();
        assertThat(mfaSessionService.isSetupRequired(session)).isFalse();
    }

    @Test
    void validStepUpGrantRequiresSameUserIpFingerprintAndSufficientScore() {
        mfaSessionService.grantStepUp(
                session,
                "admin",
                90,
                "203.0.113.10",
                "fp:chrome|windows|desktop",
                Duration.ofMinutes(10)
        );

        assertThat(mfaSessionService.hasValidStepUpGrant(
                session,
                "admin",
                90,
                "203.0.113.10",
                "fp:chrome|windows|desktop"
        )).isTrue();

        assertThat(mfaSessionService.hasValidStepUpGrant(
                session,
                "admin",
                95,
                "203.0.113.10",
                "fp:chrome|windows|desktop"
        )).isFalse();
    }

    @Test
    void stepUpGrantCanIgnoreIpAndFingerprintWhenStrictnessIsDisabled() {
        MfaStepUpProperties properties = new MfaStepUpProperties();
        properties.setValidateRequestIp(false);
        properties.setValidateFingerprint(false);
        MfaSessionService relaxedService = new MfaSessionService(properties);

        relaxedService.grantStepUp(
                session,
                "admin",
                90,
                "203.0.113.10",
                "fp:chrome|windows|desktop",
                Duration.ofMinutes(10)
        );

        assertThat(relaxedService.hasValidStepUpGrant(
                session,
                "admin",
                90,
                "198.51.100.22",
                "fp:firefox|windows|desktop"
        )).isTrue();
    }
}
