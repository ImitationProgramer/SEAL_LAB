package com.seal.seal_lab.infra.aspect;

import com.seal.seal_lab.core.annotation.ZeroTrust;
import com.seal.seal_lab.core.service.ZeroTrustService;
import com.seal.seal_lab.infra.config.ZeroTrustPolicyProperties;
import com.seal.seal_lab.infra.security.DeviceFingerprintResolver;
import com.seal.seal_lab.infra.security.MfaSessionService;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ZeroTrustAspect {

    private final ZeroTrustService zeroTrustService;
    private final ClientIpResolver clientIpResolver;
    private final MfaSessionService mfaSessionService;
    private final ZeroTrustPolicyProperties zeroTrustPolicyProperties;
    private final HttpServletRequest request;

    @Before("@annotation(zeroTrust)")
    public void enforceZeroTrust(ZeroTrust zeroTrust) {
        // 1. 현재 사용자 인증 정보 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAnonymous = (auth == null || "anonymousUser".equals(auth.getName()));

        // [CASE 1] 로그인을 하지 않은 익명 사용자 (Anonymous)
        if (isAnonymous) {
            if (zeroTrust.requiredScore() <= 0) {
                log.info("[ZTA-PASS] Anonymous User Access to Public Resource: {}", request.getRequestURI());
                return;
            } else {
                log.warn("[ZTA-BLOCK] Login required for: {}", request.getRequestURI());
                throw new AccessDeniedException("로그인이 필요한 서비스입니다.");
            }
        }

        // [CASE 2] 로그인이 된 사용자 (PDP 엔진 가동)
        String loginId = auth.getName();
        String currentIp = clientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        String currentFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(userAgent);

        // 실시간 신뢰 점수 산출 및 자가 회복 수행
        int currentScore = zeroTrustService.calculateTrustScore(loginId, currentIp, userAgent);

        // [핵심 추가] ExceptionHandler에서 사용할 수 있도록 request에 데이터 보관 (4090 에러 방지)
        request.setAttribute("ztaCurrentScore", currentScore);
        request.setAttribute("ztaRequiredScore", zeroTrust.requiredScore());

        // [위험 유저 격리] 점수가 임계치 미만이면 0점 페이지도 차단
        if (currentScore < zeroTrustPolicyProperties.getAbsoluteMinimumScore()) {
            log.error("[ZTA-CRITICAL] 위험 사용자 접근 차단! User: {} | Score: {} | URI: {}",
                    loginId, currentScore, request.getRequestURI());
            throw new AccessDeniedException("보안 위협이 감지되어 시스템 이용이 일시적으로 제한되었습니다. (신뢰 점수: " + currentScore + ")");
        }

        log.info("[ZTA-PDP] User: {} | Score: {} | Required: {}",
                loginId, currentScore, zeroTrust.requiredScore());

        // [정책 강제] 요구 점수 미달 시 차단
        if (currentScore < zeroTrust.requiredScore()) {
            HttpSession session = request.getSession(false);
            if (session != null && mfaSessionService.hasValidStepUpGrant(
                    session,
                    loginId,
                    zeroTrust.requiredScore(),
                    currentIp,
                    currentFingerprint
            )) {
                log.warn("[ZTA-STEP-UP-PASS] User: {} | Base Score: {} | Required: {} | URI: {}",
                        loginId, currentScore, zeroTrust.requiredScore(), request.getRequestURI());
                request.setAttribute("ztaStepUpApplied", true);
                return;
            }

            log.warn("[ZTA-DENY] 점수 미달로 기능 차단. User: {} | Score: {} | Required: {}",
                    loginId, currentScore, zeroTrust.requiredScore());

            // 메시지 내에 숫자가 여러 개 들어가도 Handler에서 Attribute를 우선하므로 안전합니다.
            throw new AccessDeniedException("현재 신뢰 점수(" + currentScore + ")가 요구 점수(" + zeroTrust.requiredScore() + ")보다 낮습니다.");
        }
    }
}
