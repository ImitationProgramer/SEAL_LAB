package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.infra.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AdminMfaEnforcementFilter extends OncePerRequestFilter {

    private final MfaSessionService mfaSessionService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || "anonymousUser".equals(authentication.getName()) || !isAdmin(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAllowedPendingPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        var user = userRepository.findByLoginId(authentication.getName()).orElse(null);
        if (user != null && !user.isMfaEnabled()) {
            HttpSession session = request.getSession(true);
            mfaSessionService.beginLoginVerification(session, authentication.getName(), true);
            response.sendRedirect("/mfa/setup");
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || !mfaSessionService.isLoginVerificationRequired(session)) {
            filterChain.doFilter(request, response);
            return;
        }

        String redirectUri = mfaSessionService.isSetupRequired(session) ? "/mfa/setup" : "/mfa/verify";
        response.sendRedirect(redirectUri);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private boolean isAllowedPendingPath(String uri) {
        return uri.startsWith("/mfa/")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || "/favicon.ico".equals(uri)
                || "/logout".equals(uri);
    }
}
