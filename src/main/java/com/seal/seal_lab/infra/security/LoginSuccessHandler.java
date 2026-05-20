package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.service.ZeroTrustService; // 서비스 주입 필요
import com.seal.seal_lab.infra.repository.UserRepository;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ZeroTrustService zeroTrustService; // 자가 치유 엔진 주입
    private final ClientIpResolver clientIpResolver;
    private final UserRepository userRepository;
    private final MfaSessionService mfaSessionService;
    private final MfaAuditService mfaAuditService;
    private final PasswordSessionService passwordSessionService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String loginId = authentication.getName();
        String currentIp = clientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");

        var user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + loginId));

        if (user.getRole() == com.seal.seal_lab.core.entity.User.Role.ADMIN) {
            boolean setupRequired = !user.isMfaEnabled();
            mfaSessionService.beginLoginVerification(request.getSession(true), loginId, setupRequired);
            String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(userAgent);

            log.info("[AUTH-LOGIN-PRIMARY] Admin primary authentication succeeded. User: '{}' | MFA Setup Required: {}",
                    loginId, setupRequired);
            if (setupRequired) {
                mfaAuditService.recordLoginSetupStarted(loginId, currentIp, sessionFingerprint);
            } else {
                mfaAuditService.recordLoginVerifyStarted(loginId, currentIp, sessionFingerprint);
            }

            response.sendRedirect(setupRequired ? "/mfa/setup" : "/mfa/verify");
            return;
        }

        // 1. [LOG] 로그인 시도 기록
        log.info("[AUTH-LOGIN-SUCCESS] User: '{}' | IP: {} | UA: {}", loginId, currentIp, userAgent);

        /**
         * 2. [ZTA 핵심] 직접 필드를 수정하지 않고 Service의 엔진을 호출합니다.
         * 이렇게 해야 Service 안에서 '마지막 접속 시간(어제)'과 '현재 시간'을 비교해
         * 점수를 회복(Self-Healing)시킨 후, 비로소 현재 시간으로 업데이트합니다.
         */
        int finalScore = zeroTrustService.calculateTrustScore(loginId, currentIp, userAgent);

        // 정상 인증이 완료된 세션에 한해 현재 브라우저 컨텍스트를 신뢰 기기로 등록합니다.
        zeroTrustService.registerTrustedLoginContext(loginId, currentIp, userAgent);

        // 3. [LOG] 최종 결과 기록
        log.info("[ZTA-SYNC] User: '{}' | Final Trust Score after recovery: {} | Status: Active",
                loginId, finalScore);

        passwordSessionService.markAuthenticationEstablished(request.getSession(true));
        log.info("[AUTH-REDIRECT] Session established for '{}'. Redirecting to main...", loginId);

        response.sendRedirect("/");
    }
}
