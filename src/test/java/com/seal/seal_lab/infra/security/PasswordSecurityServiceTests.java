package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordSecurityServiceTests {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private PasswordSecurityService passwordSecurityService;

    private User adminUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        passwordSecurityService = new PasswordSecurityService(userRepository, passwordEncoder);
        adminUser = User.builder()
                .loginId("admin")
                .password(passwordEncoder.encode("Oldpass1!"))
                .role(User.Role.ADMIN)
                .passwordChangedAt(LocalDateTime.now().minusDays(1))
                .build();
        memberUser = User.builder()
                .loginId("member")
                .password(passwordEncoder.encode("Oldpass1!"))
                .role(User.Role.MEMBER)
                .passwordChangedAt(LocalDateTime.now().minusDays(1))
                .build();
        lenient().when(userRepository.findByLoginId("admin")).thenReturn(Optional.of(adminUser));
        lenient().when(userRepository.findByLoginId("member")).thenReturn(Optional.of(memberUser));
    }

    @Test
    void changesPasswordAndUpdatesChangedTimestamp() {
        LocalDateTime previous = adminUser.getPasswordChangedAt();

        passwordSecurityService.changeAdminPassword("admin", "Oldpass1!", "Newpass2!", "Newpass2!");

        assertThat(passwordEncoder.matches("Newpass2!", adminUser.getPassword())).isTrue();
        assertThat(adminUser.getPasswordChangedAt()).isAfter(previous);
        verify(userRepository).save(adminUser);
    }

    @Test
    void rejectsWrongCurrentPassword() {
        assertThatThrownBy(() -> passwordSecurityService.changeAdminPassword("admin", "wrong", "Newpass2!", "Newpass2!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void memberCanChangePasswordAndUpdatesChangedTimestamp() {
        LocalDateTime previous = memberUser.getPasswordChangedAt();

        passwordSecurityService.changeMemberPassword("member", "Oldpass1!", "Newpass2!", "Newpass2!");

        assertThat(passwordEncoder.matches("Newpass2!", memberUser.getPassword())).isTrue();
        assertThat(memberUser.getPasswordChangedAt()).isAfter(previous);
        verify(userRepository).save(memberUser);
    }

    @Test
    void memberPasswordChangeRejectsWrongCurrentPassword() {
        assertThatThrownBy(() -> passwordSecurityService.changeMemberPassword("member", "wrong", "Newpass2!", "Newpass2!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 비밀번호가 올바르지 않습니다.");
    }
}
