package com.seal.seal_lab.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {

        if (authentication != null && authentication.getName() != null) {
            String loginId = authentication.getName();
            String ip = request.getRemoteAddr();

            // [LOG] 로그아웃 기록
            // 나중에 보안 감사 시 "이 사용자가 언제 나갔는지" 확인하는 용도입니다.
            log.info("[AUTH-LOGOUT] User: '{}' logged out successfully. | IP: {}", loginId, ip);
        } else {
            log.info("[AUTH-LOGOUT] Anonymous user or expired session logout.");
        }

        // 로그아웃 후 메인 페이지로 리다이렉트
        response.sendRedirect("/");
    }
}