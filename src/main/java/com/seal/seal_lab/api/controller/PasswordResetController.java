package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.infra.security.PasswordResetService;
import com.seal.seal_lab.infra.security.DeviceFingerprintResolver;
import com.seal.seal_lab.infra.security.MfaAuditService;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final MfaAuditService mfaAuditService;
    private final ClientIpResolver clientIpResolver;

    @GetMapping("/password/forgot")
    public String forgotPasswordPage() {
        return "auth/password_forgot";
    }

    @GetMapping("/password/forgot/member")
    public String forgotMemberPasswordPage() {
        return "auth/member_password_forgot";
    }

    @PostMapping("/password/forgot")
    public String issuePasswordResetToken(@RequestParam("loginId") String loginId,
                                          @RequestParam("email") String email,
                                          HttpServletRequest request,
                                          Model model) {
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        try {
            PasswordResetService.IssuedResetToken issuedToken = passwordResetService.issueAdminResetToken(loginId, email);
            mfaAuditService.recordAdminPasswordResetIssueSuccess(
                    issuedToken.loginId(), requestIp, sessionFingerprint, issuedToken.expiresAt());
            model.addAttribute("issuedToken", issuedToken.plainToken());
            model.addAttribute("loginId", issuedToken.loginId());
            model.addAttribute("expiresAt", issuedToken.expiresAt());
            return "auth/password_reset_requested";
        } catch (IllegalArgumentException ex) {
            mfaAuditService.recordAdminPasswordResetIssueFailure(loginId, requestIp, sessionFingerprint, ex.getMessage());
            model.addAttribute("resetError", ex.getMessage());
            model.addAttribute("loginId", loginId);
            model.addAttribute("email", email);
            return "auth/password_forgot";
        }
    }

    @PostMapping("/password/forgot/member")
    public String issueMemberPasswordResetToken(@RequestParam("loginId") String loginId,
                                                @RequestParam("email") String email,
                                                HttpServletRequest request,
                                                Model model) {
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        try {
            PasswordResetService.IssuedResetToken issuedToken = passwordResetService.issueMemberResetToken(loginId, email);
            mfaAuditService.recordMemberPasswordResetIssueSuccess(
                    issuedToken.loginId(), requestIp, sessionFingerprint, issuedToken.expiresAt());
            model.addAttribute("issuedToken", issuedToken.plainToken());
            model.addAttribute("loginId", issuedToken.loginId());
            model.addAttribute("expiresAt", issuedToken.expiresAt());
            return "auth/member_password_reset_requested";
        } catch (IllegalArgumentException ex) {
            mfaAuditService.recordMemberPasswordResetIssueFailure(loginId, requestIp, sessionFingerprint, ex.getMessage());
            model.addAttribute("resetError", ex.getMessage());
            model.addAttribute("loginId", loginId);
            model.addAttribute("email", email);
            return "auth/member_password_forgot";
        }
    }

    @GetMapping("/password/reset")
    public String passwordResetPage(@RequestParam("token") String token,
                                    Model model) {
        try {
            PasswordResetService.ResetTokenPreview preview = passwordResetService.validateResetToken(token);
            model.addAttribute("token", token);
            model.addAttribute("loginId", preview.loginId());
            model.addAttribute("expiresAt", preview.expiresAt());
            model.addAttribute("tokenValid", true);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("tokenValid", false);
            model.addAttribute("resetError", ex.getMessage());
        }
        return "auth/password_reset";
    }

    @GetMapping("/password/reset/member")
    public String memberPasswordResetPage(@RequestParam("token") String token,
                                          Model model) {
        try {
            PasswordResetService.ResetTokenPreview preview = passwordResetService.validateMemberResetToken(token);
            model.addAttribute("token", token);
            model.addAttribute("loginId", preview.loginId());
            model.addAttribute("expiresAt", preview.expiresAt());
            model.addAttribute("tokenValid", true);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("tokenValid", false);
            model.addAttribute("resetError", ex.getMessage());
        }
        return "auth/member_password_reset";
    }

    @PostMapping("/password/reset")
    public String resetPassword(@RequestParam("token") String token,
                                @RequestParam("backupCode") String backupCode,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        String resolvedLoginId = null;
        try {
            PasswordResetService.ResetTokenPreview preview = passwordResetService.validateResetToken(token);
            resolvedLoginId = preview.loginId();
            passwordResetService.resetAdminPassword(token, backupCode, newPassword, confirmPassword);
            mfaAuditService.recordAdminPasswordResetCompleteSuccess(preview.loginId(), requestIp, sessionFingerprint);
            return "redirect:/login?passwordReset=true";
        } catch (IllegalArgumentException ex) {
            mfaAuditService.recordAdminPasswordResetCompleteFailure(resolvedLoginId, requestIp, sessionFingerprint, ex.getMessage());
            redirectAttributes.addFlashAttribute("resetError", ex.getMessage());
            return "redirect:/password/reset?token=" + token;
        }
    }

    @PostMapping("/password/reset/member")
    public String resetMemberPassword(@RequestParam("token") String token,
                                      @RequestParam("newPassword") String newPassword,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        String resolvedLoginId = null;
        try {
            PasswordResetService.ResetTokenPreview preview = passwordResetService.validateMemberResetToken(token);
            resolvedLoginId = preview.loginId();
            passwordResetService.resetMemberPassword(token, newPassword, confirmPassword);
            mfaAuditService.recordMemberPasswordResetCompleteSuccess(preview.loginId(), requestIp, sessionFingerprint);
            return "redirect:/login?passwordReset=true";
        } catch (IllegalArgumentException ex) {
            mfaAuditService.recordMemberPasswordResetCompleteFailure(resolvedLoginId, requestIp, sessionFingerprint, ex.getMessage());
            redirectAttributes.addFlashAttribute("resetError", ex.getMessage());
            return "redirect:/password/reset/member?token=" + token;
        }
    }
}
