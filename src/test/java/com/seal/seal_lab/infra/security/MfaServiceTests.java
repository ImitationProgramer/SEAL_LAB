package com.seal.seal_lab.infra.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MfaServiceTests {

    private final MfaService mfaService = new MfaService();

    @Test
    void generatedSecretProducesVerifiableCurrentCode() {
        String secret = mfaService.generateSecret();
        long interval = Instant.now().getEpochSecond() / 30;

        String code = mfaService.generateCode(secret, interval);

        assertThat(secret).isNotBlank();
        assertThat(code).hasSize(6);
        assertThat(mfaService.verifyCode(secret, code)).isTrue();
    }

    @Test
    void invalidCodeIsRejected() {
        String secret = mfaService.generateSecret();

        assertThat(mfaService.verifyCode(secret, "00000A")).isFalse();
        assertThat(mfaService.verifyCode(secret, "12345")).isFalse();
    }
}
