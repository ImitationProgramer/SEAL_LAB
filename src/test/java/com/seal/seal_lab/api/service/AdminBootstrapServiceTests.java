package com.seal.seal_lab.api.service;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.config.AdminBootstrapProperties;
import com.seal.seal_lab.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTests {

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AdminBootstrapProperties properties;

    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        properties = new AdminBootstrapProperties();
        properties.setEnabled(true);
        properties.setLoginId("jspark0427");
        properties.setPassword("Initpass1!");
        adminBootstrapService = new AdminBootstrapService(userRepository, passwordEncoder, properties);
    }

    @Test
    void createsInitialAdminWhenMissing() throws Exception {
        when(userRepository.findByLoginId("jspark0427")).thenReturn(Optional.empty());

        adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

        var captor = forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getLoginId()).isEqualTo("jspark0427");
        assertThat(saved.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(saved.getResolvedLabRank()).isEqualTo(User.LabRank.PROFESSOR);
        assertThat(passwordEncoder.matches("Initpass1!", saved.getPassword())).isTrue();
    }

    @Test
    void skipsWhenAdminAlreadyExists() throws Exception {
        when(userRepository.findByLoginId("jspark0427")).thenReturn(Optional.of(User.builder()
                .loginId("jspark0427")
                .role(User.Role.ADMIN)
                .trustScore(100)
                .build()));

        adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsWhenDisabled() throws Exception {
        properties.setEnabled(false);

        adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).findByLoginId(org.mockito.ArgumentMatchers.anyString());
    }
}
