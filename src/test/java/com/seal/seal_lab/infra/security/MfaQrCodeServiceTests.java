package com.seal.seal_lab.infra.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MfaQrCodeServiceTests {

    private final MfaQrCodeService mfaQrCodeService = new MfaQrCodeService();

    @Test
    void generatesNonEmptyPngQrCode() {
        byte[] png = mfaQrCodeService.generatePng("otpauth://totp/SEAL_LAB:admin?secret=ABC123&issuer=SEAL_LAB", 280, 280);

        assertThat(png).isNotEmpty();
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50);
        assertThat(png[2]).isEqualTo((byte) 0x4E);
        assertThat(png[3]).isEqualTo((byte) 0x47);
    }
}
