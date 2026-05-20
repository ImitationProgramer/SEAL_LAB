package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.PasswordResetToken;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.config.PasswordResetProperties;
import com.seal.seal_lab.infra.repository.PasswordResetTokenRepository;
import com.seal.seal_lab.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTests {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MfaBackupCodeService mfaBackupCodeService;

    @Mock
    private PasswordSecurityService passwordSecurityService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setTokenTtlMinutes(15);
        passwordResetService = new PasswordResetService(
                passwordResetTokenRepository,
                userRepository,
                mfaBackupCodeService,
                passwordSecurityService,
                properties
        );
    }

    @Test
    void issuesResetTokenForMatchingAdminAccount() {
        when(userRepository.findByLoginId("admin")).thenReturn(Optional.of(
                User.builder()
                        .loginId("admin")
                        .email("admin@example.com")
                        .role(User.Role.ADMIN)
                        .build()
        ));

        PasswordResetService.IssuedResetToken token = passwordResetService.issueAdminResetToken("admin", "admin@example.com");

        assertThat(token.loginId()).isEqualTo("admin");
        assertThat(token.plainToken()).hasSize(24);
        verify(passwordResetTokenRepository).deleteByLoginIdAndPurposeAndUsedAtIsNull("admin", PasswordResetToken.Purpose.ADMIN_FORGOT_PASSWORD);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void resetConsumesBackupCodeAndMarksTokenUsed() {
        PasswordResetToken token = PasswordResetToken.builder()
                .loginId("admin")
                .tokenHash("hash")
                .purpose(PasswordResetToken.Purpose.ADMIN_FORGOT_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));
        when(mfaBackupCodeService.consumeCode("admin", "ABCD-EFGH")).thenReturn(true);

        passwordResetService.resetAdminPassword("ABCD2345EFGH6789JKLM2345", "ABCD-EFGH", "Newpass2!", "Newpass2!");

        verify(passwordSecurityService).resetAdminPasswordWithoutCurrent("admin", "Newpass2!", "Newpass2!");
        assertThat(token.getUsedAt()).isNotNull();
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void invalidBackupCodeBlocksPasswordReset() {
        PasswordResetToken token = PasswordResetToken.builder()
                .loginId("admin")
                .tokenHash("hash")
                .purpose(PasswordResetToken.Purpose.ADMIN_FORGOT_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));
        when(mfaBackupCodeService.consumeCode("admin", "ABCD-EFGH")).thenReturn(false);

        assertThatThrownBy(() -> passwordResetService.resetAdminPassword("ABCD2345EFGH6789JKLM2345", "ABCD-EFGH", "Newpass2!", "Newpass2!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않거나 이미 사용된 backup code입니다.");
    }

    @Test
    void issuesResetTokenForMatchingMemberAccount() {
        when(userRepository.findByLoginId("member")).thenReturn(Optional.of(
                User.builder()
                        .loginId("member")
                        .email("member@example.com")
                        .role(User.Role.MEMBER)
                        .build()
        ));

        PasswordResetService.IssuedResetToken token = passwordResetService.issueMemberResetToken("member", "member@example.com");

        assertThat(token.loginId()).isEqualTo("member");
        assertThat(token.plainToken()).hasSize(24);
        verify(passwordResetTokenRepository).deleteByLoginIdAndPurposeAndUsedAtIsNull("member", PasswordResetToken.Purpose.MEMBER_FORGOT_PASSWORD);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void memberResetUsesMemberPasswordUpdateWithoutBackupCode() {
        PasswordResetToken token = PasswordResetToken.builder()
                .loginId("member")
                .tokenHash("hash")
                .purpose(PasswordResetToken.Purpose.MEMBER_FORGOT_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedAtIsNull(any())).thenReturn(Optional.of(token));

        passwordResetService.resetMemberPassword("ABCD2345EFGH6789JKLM2345", "Newpass2!", "Newpass2!");

        verify(passwordSecurityService).resetMemberPasswordWithoutCurrent("member", "Newpass2!", "Newpass2!");
        assertThat(token.getUsedAt()).isNotNull();
        verify(passwordResetTokenRepository).save(token);
    }
}
