package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.infra.logging.AdminOperationLogFormatter;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ClientIpResolver clientIpResolver;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loginId = (authentication != null) ? authentication.getName() : "anonymousUser";

        request.setAttribute("denialType", "ROLE");
        request.setAttribute("userName", loginId);
        request.setAttribute("errorMessage", "관리자 권한이 필요한 기능입니다.");

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        if (isTrustAdminOperation(request)) {
            log.warn("{}", AdminOperationLogFormatter.formatFailure(
                    resolveAdminAction(request),
                    loginId,
                    request.getParameter("loginId"),
                    "ROLE_ADMIN required",
                    request.getParameter("reason"),
                    request.getParameter("trustScore"),
                    request.getParameter("clearContext"),
                    clientIpResolver.resolve(request),
                    DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"))
            ));
        } else {
            log.warn("[AUTH-DENIED] User: {} | URI: {} | Reason: ROLE_ADMIN required",
                    loginId, request.getRequestURI());
        }

        request.getRequestDispatcher("/access-denied").forward(request, response);
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
