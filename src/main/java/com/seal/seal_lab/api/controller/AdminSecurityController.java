package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.infra.security.PasswordSecurityService;
import com.seal.seal_lab.infra.security.DeviceFingerprintResolver;
import com.seal.seal_lab.infra.security.MfaAuditService;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AdminSecurityController {

    private final PasswordSecurityService passwordSecurityService;
    private final MfaAuditService mfaAuditService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/admin/security/password")
    public String passwordChangeForm(Authentication authentication, Model model) {
        model.addAttribute("loginId", authentication != null ? authentication.getName() : "admin");
        return "admin/password_change";
    }

    @PostMapping("/admin/security/password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String loginId = authentication.getName();
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        try {
            passwordSecurityService.changeAdminPassword(loginId, currentPassword, newPassword, confirmPassword);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mfaAuditService.recordAdminPasswordChangeFailure(loginId, requestIp, sessionFingerprint, ex.getMessage());
            redirectAttributes.addFlashAttribute("passwordError", ex.getMessage());
            return "redirect:/admin/security/password";
        }

        mfaAuditService.recordAdminPasswordChangeSuccess(loginId, requestIp, sessionFingerprint);

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        log.warn("[AUTH-PASSWORD-ROTATED] User: {} | Current session invalidated after password change.", loginId);
        return "redirect:/login?passwordChanged=true";
    }
}
