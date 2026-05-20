package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.PasswordResetToken;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.config.PasswordResetProperties;
import com.seal.seal_lab.infra.repository.PasswordResetTokenRepository;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final char[] TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int TOKEN_LENGTH = 24;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final MfaBackupCodeService mfaBackupCodeService;
    private final PasswordSecurityService passwordSecurityService;
    private final PasswordResetProperties passwordResetProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IssuedResetToken issueAdminResetToken(String loginId, String email) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("입력한 관리자 계정을 확인해주세요."));

        if (user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("관리자 계정만 비밀번호 reset을 지원합니다.");
        }
        if (email == null || email.isBlank() || user.getEmail() == null
                || !user.getEmail().trim().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("입력한 관리자 계정을 확인해주세요.");
        }

        passwordResetTokenRepository.deleteByLoginIdAndPurposeAndUsedAtIsNull(
                user.getLoginId(),
                PasswordResetToken.Purpose.ADMIN_FORGOT_PASSWORD
        );

        String plainToken = generatePlainToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(passwordResetProperties.getTokenTtlMinutes());
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .loginId(user.getLoginId())
                .tokenHash(hash(plainToken))
                .purpose(PasswordResetToken.Purpose.ADMIN_FORGOT_PASSWORD)
                .expiresAt(expiresAt)
                .build());

        return new IssuedResetToken(user.getLoginId(), plainToken, expiresAt);
    }

    @Transactional
    public IssuedResetToken issueMemberResetToken(String loginId, String email) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("입력한 회원 계정을 확인해주세요."));

        if (user.getRole() != User.Role.MEMBER) {
            throw new IllegalArgumentException("일반 회원 계정만 이 경로를 사용할 수 있습니다.");
        }
        if (email == null || email.isBlank() || user.getEmail() == null
                || !user.getEmail().trim().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("입력한 회원 계정을 확인해주세요.");
        }

        passwordResetTokenRepository.deleteByLoginIdAndPurposeAndUsedAtIsNull(
                user.getLoginId(),
                PasswordResetToken.Purpose.MEMBER_FORGOT_PASSWORD
        );

        String plainToken = generatePlainToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(passwordResetProperties.getTokenTtlMinutes());
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .loginId(user.getLoginId())
                .tokenHash(hash(plainToken))
                .purpose(PasswordResetToken.Purpose.MEMBER_FORGOT_PASSWORD)
                .expiresAt(expiresAt)
                .build());

        return new IssuedResetToken(user.getLoginId(), plainToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public ResetTokenPreview validateResetToken(String rawToken) {
        PasswordResetToken token = findValidToken(rawToken, PasswordResetToken.Purpose.ADMIN_FORGOT_PASSWORD);
        return new ResetTokenPreview(token.getLoginId(), token.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public ResetTokenPreview validateMemberResetToken(String rawToken) {
        PasswordResetToken token = findValidToken(rawToken, PasswordResetToken.Purpose.MEMBER_FORGOT_PASSWORD);
        return new ResetTokenPreview(token.getLoginId(), token.getExpiresAt());
    }

    @Transactional
    public void resetAdminPassword(String rawToken,
                                   String backupCode,
                                   String newPassword,
                                   String confirmPassword) {
        PasswordResetToken token = findValidToken(rawToken, PasswordResetToken.Purpose.ADMIN_FORGOT_PASSWORD);
        if (backupCode == null || backupCode.isBlank()
                || !mfaBackupCodeService.consumeCode(token.getLoginId(), backupCode)) {
            throw new IllegalArgumentException("유효하지 않거나 이미 사용된 backup code입니다.");
        }

        passwordSecurityService.resetAdminPasswordWithoutCurrent(
                token.getLoginId(),
                newPassword,
                confirmPassword
        );
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }

    @Transactional
    public void resetMemberPassword(String rawToken,
                                    String newPassword,
                                    String confirmPassword) {
        PasswordResetToken token = findValidToken(rawToken, PasswordResetToken.Purpose.MEMBER_FORGOT_PASSWORD);
        passwordSecurityService.resetMemberPasswordWithoutCurrent(
                token.getLoginId(),
                newPassword,
                confirmPassword
        );
        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);
    }

    private PasswordResetToken findValidToken(String rawToken, PasswordResetToken.Purpose expectedPurpose) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("reset token이 필요합니다.");
        }

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(hash(rawToken.trim()))
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않거나 이미 사용된 reset token입니다."));

        if (token.getPurpose() != expectedPurpose) {
            throw new IllegalArgumentException("reset token 용도가 일치하지 않습니다.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("reset token이 만료되었습니다.");
        }
        return token;
    }

    private String generatePlainToken() {
        StringBuilder builder = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            builder.append(TOKEN_CHARS[secureRandom.nextInt(TOKEN_CHARS.length)]);
        }
        return builder.toString();
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("reset token hash 생성에 실패했습니다.", e);
        }
    }

    public record IssuedResetToken(String loginId, String plainToken, LocalDateTime expiresAt) {
    }

    public record ResetTokenPreview(String loginId, LocalDateTime expiresAt) {
    }
}
