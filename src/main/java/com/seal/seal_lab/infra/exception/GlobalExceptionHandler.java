package com.seal.seal_lab.infra.exception;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.config.ZeroTrustPolicyProperties;
import com.seal.seal_lab.infra.logging.AdminOperationLogFormatter;
import com.seal.seal_lab.infra.repository.UserRepository;
import com.seal.seal_lab.infra.security.DeviceFingerprintResolver;
import com.seal.seal_lab.infra.security.MfaSessionService;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final UserRepository userRepository;
    private final ClientIpResolver clientIpResolver;
    private final MfaSessionService mfaSessionService;
    private final ZeroTrustPolicyProperties zeroTrustPolicyProperties;
    private final HttpServletRequest request; // Aspect에서 담은 데이터를 꺼내기 위해 주입

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException e, Model model) {

        // 1. 사용자 정보 식별
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loginId = (authentication != null) ? authentication.getName() : "anonymousUser";
        User user = userRepository.findByLoginId(loginId).orElse(null);
        String displayName = (user != null) ? user.getName() : loginId;

        // 2. Aspect에서 넘겨준 Zero Trust 데이터 추출 (4090 버그 원천 차단)
        Object scoreAttr = request.getAttribute("ztaCurrentScore");
        Object requiredAttr = request.getAttribute("ztaRequiredScore");

        // 만약 ZTA에 의한 차단이 아닐 경우(일반 권한 부족 등)를 대비해 기본값 설정
        String currentScore = (scoreAttr != null) ? scoreAttr.toString() : null;
        String requiredScore = (requiredAttr != null) ? requiredAttr.toString() : null;

        // 3. 뷰(HTML)에 전달할 데이터 담기
        model.addAttribute("denialType", scoreAttr != null ? "TRUST" : "AUTH");
        model.addAttribute("userName", displayName);
        model.addAttribute("trustScore", currentScore);      // 현재 유저 점수
        model.addAttribute("requiredScore", requiredScore);  // 해당 페이지 요구 점수
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("showStepUpButton", false);

        // 4. 보안 감사 로그 (구분자 '/'를 넣어 40/90 처럼 보이게 함)
        if (isTrustAdminOperation(request)) {
            log.warn("{}", AdminOperationLogFormatter.formatTrustDenied(
                    resolveAdminAction(request),
                    loginId,
                    request.getParameter("loginId"),
                    currentScore,
                    requiredScore,
                    request.getParameter("reason"),
                    request.getParameter("trustScore"),
                    request.getParameter("clearContext"),
                    clientIpResolver.resolve(request),
                    DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent")),
                    e.getMessage()
            ));
        } else {
            log.warn("[ZTA-Enforcement] User: {} | Score: {}/{} | Blocked at: {}",
                    loginId,
                    currentScore != null ? currentScore : "N/A",
                    requiredScore != null ? requiredScore : "N/A",
                    request.getRequestURI());
        }

        if (isStepUpEligible(authentication, user, currentScore, requiredScore)) {
            HttpSession session = request.getSession(true);
            String resolvedIp = clientIpResolver.resolve(request);
            String fingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
            mfaSessionService.storeStepUpCandidate(
                    session,
                    loginId,
                    Integer.parseInt(currentScore),
                    Integer.parseInt(requiredScore),
                    resolveStepUpReturnUri(request),
                    resolvedIp,
                    fingerprint
            );

            model.addAttribute("showStepUpButton", true);
            model.addAttribute("stepUpTargetUri", resolveStepUpReturnUri(request));
            model.addAttribute("stepUpReturnRequiresRetry", !"GET".equalsIgnoreCase(request.getMethod()));
        }

        return "error/access-denied"; // 차단 전용 페이지로 이동
    }

    private boolean isStepUpEligible(Authentication authentication, User user, String currentScore, String requiredScore) {
        if (authentication == null || user == null || user.getRole() != User.Role.ADMIN || !user.isMfaEnabled()) {
            return false;
        }
        if (currentScore == null || requiredScore == null) {
            return false;
        }
        try {
            int current = Integer.parseInt(currentScore);
            int required = Integer.parseInt(requiredScore);
            return current >= zeroTrustPolicyProperties.getAbsoluteMinimumScore() && current < required;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String resolveStepUpReturnUri(HttpServletRequest request) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String query = request.getQueryString();
            return query == null || query.isBlank()
                    ? request.getRequestURI()
                    : request.getRequestURI() + "?" + query;
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            if (referer.startsWith("/")) {
                return referer;
            }
            try {
                URI refererUri = URI.create(referer);
                if (request.getServerName().equalsIgnoreCase(refererUri.getHost())) {
                    String query = refererUri.getQuery();
                    return query == null || query.isBlank()
                            ? refererUri.getPath()
                            : refererUri.getPath() + "?" + query;
                }
            } catch (IllegalArgumentException ignored) {
                // fallback below
            }
        }
        return "/";
    }

    private boolean isTrustAdminOperation(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "/admin/trust/override".equals(uri) || "/admin/trust/test-baseline".equals(uri);
    }

    private String resolveAdminAction(HttpServletRequest request) {
        return "/admin/trust/test-baseline".equals(request.getRequestURI())
                ? "TEST_BASELINE_RESET"
                : "MANUAL_OVERRIDE";
    }
}
