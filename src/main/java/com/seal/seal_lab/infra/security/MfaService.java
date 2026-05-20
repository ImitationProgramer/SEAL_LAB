package com.seal.seal_lab.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

@Service
public class MfaService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES = 20;
    private static final int OTP_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CLOCK_SKEW_WINDOW = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${mfa.issuer:SEAL_LAB}")
    private String issuer;

    public String generateSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secret);
        return encodeBase32(secret);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }

        long interval = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (int offset = -CLOCK_SKEW_WINDOW; offset <= CLOCK_SKEW_WINDOW; offset++) {
            if (generateCode(secret, interval + offset).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public String buildOtpAuthUri(String accountName, String secret) {
        return "otpauth://totp/" + issuer + ":" + accountName
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&digits=" + OTP_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    String generateCode(String secret, long interval) {
        try {
            byte[] decodedSecret = decodeBase32(secret);
            byte[] counter = ByteBuffer.allocate(8).putLong(interval).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodedSecret, "HmacSHA1"));
            byte[] hash = mac.doFinal(counter);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, OTP_DIGITS);
            return String.format(Locale.ROOT, "%0" + OTP_DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TOTP code generation failed", e);
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder encoded = new StringBuilder();
        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;

        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= data[next++] & 0xFF;
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            int index = (buffer >> (bitsLeft - 5)) & 0x1F;
            bitsLeft -= 5;
            encoded.append(BASE32_ALPHABET.charAt(index));
        }
        return encoded.toString();
    }

    private byte[] decodeBase32(String encoded) {
        String normalized = encoded.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        byte[] bytes = new byte[normalized.length() * 5 / 8];

        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;

        for (char c : normalized.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base32 character");
            }
            buffer <<= 5;
            buffer |= value & 0x1F;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                bytes[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return bytes;
    }
}
