package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMfaResetServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MfaBackupCodeService mfaBackupCodeService;

    @InjectMocks
    private AdminMfaResetService adminMfaResetService;

    @Test
    void resetClearsAdminMfaStateAndBackupCodes() {
        User adminUser = User.builder()
                .loginId("admin")
                .role(User.Role.ADMIN)
                .mfaEnabled(true)
                .mfaSecret("enc:v1:abc")
                .mfaEnrolledAt(LocalDateTime.now())
                .build();

        when(userRepository.findByLoginId("admin")).thenReturn(Optional.of(adminUser));

        adminMfaResetService.resetAdminMfa("admin");

        assertThat(adminUser.isMfaEnabled()).isFalse();
        assertThat(adminUser.getMfaSecret()).isNull();
        assertThat(adminUser.getMfaEnrolledAt()).isNull();
        verify(userRepository).save(adminUser);
        verify(mfaBackupCodeService).clearAllCodes("admin");
    }

    @Test
    void resetRejectsNonAdminTargets() {
        User memberUser = User.builder()
                .loginId("member")
                .role(User.Role.MEMBER)
                .build();

        when(userRepository.findByLoginId("member")).thenReturn(Optional.of(memberUser));

        assertThatThrownBy(() -> adminMfaResetService.resetAdminMfa("member"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리자 계정만 MFA reset 대상이 될 수 있습니다.");
    }
}
