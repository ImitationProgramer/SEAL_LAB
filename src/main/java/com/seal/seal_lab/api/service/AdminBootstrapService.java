package com.seal.seal_lab.api.service;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.config.AdminBootstrapProperties;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapService implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties adminBootstrapProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!adminBootstrapProperties.isEnabled()) {
            log.info("[ADMIN-BOOTSTRAP] disabled. skipping.");
            return;
        }

        String loginId = adminBootstrapProperties.getLoginId();
        String password = adminBootstrapProperties.getPassword();

        if (!StringUtils.hasText(loginId)) {
            log.warn("[ADMIN-BOOTSTRAP] loginId is blank. skipping.");
            return;
        }
        if (!StringUtils.hasText(password)) {
            log.warn("[ADMIN-BOOTSTRAP] initial password is blank. skipping.");
            return;
        }
        if (userRepository.findByLoginId(loginId).isPresent()) {
            log.info("[ADMIN-BOOTSTRAP] admin user already exists. loginId={}", loginId);
            return;
        }

        User admin = User.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(password))
                .name("Professor")
                .email(loginId + "@example.com")
                .role(User.Role.ADMIN)
                .labRank(User.LabRank.PROFESSOR)
                .trustScore(100)
                .build();

        userRepository.save(admin);
        log.warn("[ADMIN-BOOTSTRAP] initial admin account provisioned. loginId={} role=ADMIN labRank=PROFESSOR", loginId);
    }
}
