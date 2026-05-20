package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordSessionRevocationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final PasswordSessionService passwordSessionService;
    private final MfaAuditService mfaAuditService;
    private final ClientIpResolver clientIpResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || isAllowedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByLoginId(authentication.getName()).orElse(null);
        if (user == null || !passwordSessionService.isSessionStale(session, user.getPasswordChangedAt())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        mfaAuditService.recordPasswordSessionRevoked(authentication.getName(), requestIp, sessionFingerprint, request.getRequestURI());
        log.warn("[AUTH-SESSION-REVOKED] User: {} | URI: {}", authentication.getName(), request.getRequestURI());
        session.invalidate();
        SecurityContextHolder.clearContext();
        response.sendRedirect("/login?sessionRevoked=true");
    }

    private boolean isAllowedPath(String uri) {
        return uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || "/favicon.ico".equals(uri);
    }
}
