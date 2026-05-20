package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordSecurityService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changeAdminPassword(String loginId,
                                    String currentPassword,
                                    String newPassword,
                                    String confirmPassword) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + loginId));

        if (user.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException("관리자 계정만 이 경로를 사용할 수 있습니다.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해주세요.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        validateNewPassword(newPassword, confirmPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void changeMemberPassword(String loginId,
                                     String currentPassword,
                                     String newPassword,
                                     String confirmPassword) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + loginId));

        if (user.getRole() != User.Role.MEMBER) {
            throw new IllegalStateException("일반 회원 계정만 이 경로를 사용할 수 있습니다.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해주세요.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        validateNewPassword(newPassword, confirmPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void resetAdminPasswordWithoutCurrent(String loginId,
                                                 String newPassword,
                                                 String confirmPassword) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + loginId));

        if (user.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException("관리자 계정만 이 경로를 사용할 수 있습니다.");
        }
        validateNewPassword(newPassword, confirmPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void resetMemberPasswordWithoutCurrent(String loginId,
                                                  String newPassword,
                                                  String confirmPassword) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + loginId));

        if (user.getRole() != User.Role.MEMBER) {
            throw new IllegalStateException("일반 회원 계정만 이 경로를 사용할 수 있습니다.");
        }
        validateNewPassword(newPassword, confirmPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new IllegalArgumentException("비밀번호는 영문, 숫자, 특수문자를 포함한 8자 이상이어야 합니다.");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호 확인을 입력해주세요.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호와 확인 값이 일치하지 않습니다.");
        }
    }
}
