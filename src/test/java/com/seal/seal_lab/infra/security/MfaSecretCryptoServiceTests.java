package com.seal.seal_lab.infra.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MfaSecretCryptoServiceTests {

    private final MfaSecretCryptoService cryptoService =
            new MfaSecretCryptoService("test-mfa-encryption-key");

    @Test
    void encryptsAndDecryptsMfaSecret() {
        String encrypted = cryptoService.encrypt("JBSWY3DPEHPK3PXP");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).isNotEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(cryptoService.decryptIfNeeded(encrypted)).isEqualTo("JBSWY3DPEHPK3PXP");
    }

    @Test
    void keepsLegacyPlaintextSecretReadable() {
        assertThat(cryptoService.decryptIfNeeded("JBSWY3DPEHPK3PXP")).isEqualTo("JBSWY3DPEHPK3PXP");
    }
}
