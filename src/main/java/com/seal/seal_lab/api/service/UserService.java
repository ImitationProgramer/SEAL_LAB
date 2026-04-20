package com.seal.seal_lab.api.service;

import com.seal.seal_lab.api.dto.UserSignupDto;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 신규 연구원(Member) 등록 로직
     * ZTA 원칙: 모든 사용자는 초기 신뢰 상태(Baseline Trust)에서 시작함
     */
    public void register(UserSignupDto dto) {
        // 1. 아이디 중복 체크 (신원 고유성 검증)
        userRepository.findByLoginId(dto.getLoginId()).ifPresent(user -> {
            log.warn("[Signup Failed] 이미 존재하는 아이디입니다: {}", dto.getLoginId());
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        });

        // 2. DTO -> Entity 변환 및 초기 보안 설정
        User user = User.builder()
                .loginId(dto.getLoginId())
                .password(passwordEncoder.encode(dto.getPassword())) // 암호화 저장
                .name(dto.getName())
                .email(dto.getEmail())
                .role(User.Role.MEMBER)
                .trustScore(100) // [ZTA Policy] 초기 가입 시 만점(100) 부여 후 활동에 따라 동적 감점
                .build();

        // 3. DB 저장 및 로그 기록
        userRepository.save(user);

        log.info("[ZTA-Provisioning] 새 연구원 가입 완료: ID={}, Initial Score=100", user.getLoginId());
    }
}