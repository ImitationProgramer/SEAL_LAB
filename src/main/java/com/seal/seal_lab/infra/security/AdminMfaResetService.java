package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AdminMfaResetService {

    private final UserRepository userRepository;
    private final MfaBackupCodeService mfaBackupCodeService;

    @Transactional
    public void resetAdminMfa(String targetLoginId) {
        if (targetLoginId == null || targetLoginId.isBlank()) {
            throw new IllegalArgumentException("대상 loginId는 비워둘 수 없습니다.");
        }

        User user = userRepository.findByLoginId(targetLoginId.trim())
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + targetLoginId));

        if (user.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("관리자 계정만 MFA reset 대상이 될 수 있습니다.");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaEnrolledAt(null);
        userRepository.save(user);
        mfaBackupCodeService.clearAllCodes(user.getLoginId());
    }
}
