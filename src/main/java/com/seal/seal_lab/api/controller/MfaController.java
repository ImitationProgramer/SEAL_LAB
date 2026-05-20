package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.core.service.ZeroTrustService;
import com.seal.seal_lab.infra.config.MfaStepUpProperties;
import com.seal.seal_lab.infra.repository.UserRepository;
import com.seal.seal_lab.infra.security.DeviceFingerprintResolver;
import com.seal.seal_lab.infra.security.MfaAuditService;
import com.seal.seal_lab.infra.security.MfaBackupCodeService;
import com.seal.seal_lab.infra.security.MfaService;
import com.seal.seal_lab.infra.security.MfaSecretCryptoService;
import com.seal.seal_lab.infra.security.MfaSessionService;
import com.seal.seal_lab.infra.security.MfaQrCodeService;
import com.seal.seal_lab.infra.security.PasswordSessionService;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MfaController {

    private final UserRepository userRepository;
    private final ZeroTrustService zeroTrustService;
    private final MfaService mfaService;
    private final MfaSessionService mfaSessionService;
    private final MfaAuditService mfaAuditService;
    private final MfaBackupCodeService mfaBackupCodeService;
    private final MfaSecretCryptoService mfaSecretCryptoService;
    private final MfaQrCodeService mfaQrCodeService;
    private final PasswordSessionService passwordSessionService;
    private final ClientIpResolver clientIpResolver;
    private final MfaStepUpProperties mfaStepUpProperties;

    @GetMapping("/mfa/setup")
    public String mfaSetup(Authentication authentication, HttpSession session, Model model) {
        User user = requireAdminUser(authentication);
        if (user.isMfaEnabled() && !mfaSessionService.isLoginVerificationRequired(session)) {
            return "redirect:/";
        }

        String secret = resolveOrCreatePendingSetupSecret(session);

        model.addAttribute("loginId", user.getLoginId());
        model.addAttribute("secret", secret);
        model.addAttribute("otpAuthUri", mfaService.buildOtpAuthUri(user.getLoginId(), secret));
        return "auth/mfa_setup";
    }

    @GetMapping(value = "/mfa/setup/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] mfaSetupQr(Authentication authentication, HttpSession session) {
        User user = requireAdminUser(authentication);
        String secret = resolveOrCreatePendingSetupSecret(session);
        String otpAuthUri = mfaService.buildOtpAuthUri(user.getLoginId(), secret);
        return mfaQrCodeService.generatePng(otpAuthUri, 280, 280);
    }

    @PostMapping("/mfa/setup")
    public String completeMfaSetup(@RequestParam("code") String code,
                                   Authentication authentication,
                                   HttpServletRequest request,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = requireAdminUser(authentication);
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        String secret = mfaSessionService.getPendingSetupSecret(session);
        if (secret == null) {
            mfaAuditService.recordLoginSetupFailure(user.getLoginId(), requestIp, sessionFingerprint,
                    "MFA 설정 세션이 만료되었습니다.");
            redirectAttributes.addFlashAttribute("mfaError", "MFA 설정 세션이 만료되었습니다. 다시 로그인해주세요.");
            return "redirect:/mfa/setup";
        }
        if (!mfaService.verifyCode(secret, code)) {
            mfaAuditService.recordLoginSetupFailure(user.getLoginId(), requestIp, sessionFingerprint,
                    "유효하지 않은 MFA 코드입니다.");
            redirectAttributes.addFlashAttribute("mfaError", "유효하지 않은 MFA 코드입니다.");
            return "redirect:/mfa/setup";
        }

        user.setMfaEnabled(true);
        user.setMfaSecret(mfaSecretCryptoService.encrypt(secret));
        user.setMfaEnrolledAt(LocalDateTime.now());
        userRepository.save(user);

        mfaSessionService.markLoginVerified(session);
        mfaSessionService.storeGeneratedBackupCodes(session, mfaBackupCodeService.issueNewCodes(user.getLoginId()));
        mfaAuditService.recordLoginSetupSuccess(user.getLoginId(), requestIp, sessionFingerprint);

        int finalScore = zeroTrustService.finalizeSuccessfulLogin(
                user.getLoginId(),
                requestIp,
                request.getHeader("User-Agent")
        );

        passwordSessionService.markAuthenticationEstablished(session);
        log.info("[MFA-SETUP-COMPLETED] User: {} | Final Score: {}", user.getLoginId(), finalScore);
        return "redirect:/mfa/recovery/codes";
    }

    @GetMapping("/mfa/verify")
    public String mfaVerify(Authentication authentication, HttpSession session, Model model) {
        User user = requireAdminUser(authentication);

        if (mfaSessionService.isStepUpPending(session)) {
            MfaSessionService.StepUpCandidate candidate = mfaSessionService.getStepUpCandidate(session);
            model.addAttribute("mode", "STEP_UP");
            model.addAttribute("requiredScore", candidate != null ? candidate.requiredScore() : null);
            model.addAttribute("currentScore", candidate != null ? candidate.currentScore() : null);
            model.addAttribute("returnUri", candidate != null ? candidate.returnUri() : "/");
            model.addAttribute("loginId", user.getLoginId());
            return "auth/mfa_verify";
        }

        if (mfaSessionService.isLoginVerificationRequired(session) && user.isMfaEnabled()) {
            model.addAttribute("mode", "LOGIN");
            model.addAttribute("loginId", user.getLoginId());
            return "auth/mfa_verify";
        }

        return mfaSessionService.isSetupRequired(session) ? "redirect:/mfa/setup" : "redirect:/";
    }

    @GetMapping("/mfa/recovery")
    public String mfaRecovery(Authentication authentication,
                              HttpServletRequest request,
                              HttpSession session,
                              Model model) {
        User user = requireAdminUser(authentication);
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));

        if (mfaSessionService.isStepUpPending(session)) {
            MfaSessionService.StepUpCandidate candidate = mfaSessionService.getStepUpCandidate(session);
            model.addAttribute("mode", "STEP_UP");
            model.addAttribute("requiredScore", candidate != null ? candidate.requiredScore() : null);
            model.addAttribute("currentScore", candidate != null ? candidate.currentScore() : null);
            model.addAttribute("loginId", user.getLoginId());
            mfaAuditService.recordBackupCodeStepUpStarted(
                    user.getLoginId(),
                    candidate != null ? candidate.currentScore() : null,
                    candidate != null ? candidate.requiredScore() : null,
                    requestIp,
                    sessionFingerprint,
                    candidate != null ? candidate.returnUri() : null
            );
            return "auth/mfa_recovery";
        }

        if (mfaSessionService.isLoginVerificationRequired(session)) {
            model.addAttribute("mode", "LOGIN");
            model.addAttribute("loginId", user.getLoginId());
            mfaAuditService.recordBackupCodeLoginStarted(user.getLoginId(), requestIp, sessionFingerprint);
            return "auth/mfa_recovery";
        }

        return "redirect:/";
    }

    @PostMapping("/mfa/recovery")
    public String verifyBackupCode(@RequestParam("backupCode") String backupCode,
                                   Authentication authentication,
                                   HttpServletRequest request,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User user = requireAdminUser(authentication);
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        boolean isStepUpFlow = mfaSessionService.isStepUpPending(session);
        MfaSessionService.StepUpCandidate candidate = isStepUpFlow ? mfaSessionService.getStepUpCandidate(session) : null;

        if (!mfaBackupCodeService.consumeCode(user.getLoginId(), backupCode)) {
            if (isStepUpFlow) {
                mfaAuditService.recordBackupCodeStepUpFailure(
                        user.getLoginId(),
                        candidate != null ? candidate.currentScore() : null,
                        candidate != null ? candidate.requiredScore() : null,
                        requestIp,
                        sessionFingerprint,
                        candidate != null ? candidate.returnUri() : null,
                        "유효하지 않거나 이미 사용된 backup code입니다."
                );
            } else {
                mfaAuditService.recordBackupCodeLoginFailure(
                        user.getLoginId(),
                        requestIp,
                        sessionFingerprint,
                        "유효하지 않거나 이미 사용된 backup code입니다."
                );
            }
            redirectAttributes.addFlashAttribute("mfaError", "유효하지 않거나 이미 사용된 backup code입니다.");
            return "redirect:/mfa/recovery";
        }

        long remainingCodes = mfaBackupCodeService.countRemainingCodes(user.getLoginId());

        if (isStepUpFlow) {
            if (candidate == null || !user.getLoginId().equals(candidate.loginId())) {
                mfaAuditService.recordBackupCodeStepUpFailure(
                        user.getLoginId(),
                        null,
                        null,
                        requestIp,
                        sessionFingerprint,
                        null,
                        "Step-up recovery 세션이 유효하지 않습니다."
                );
                redirectAttributes.addFlashAttribute("mfaError", "Step-up recovery 세션이 유효하지 않습니다.");
                return "redirect:/trust/debug";
            }

            mfaSessionService.grantStepUp(
                    session,
                    user.getLoginId(),
                    candidate.requiredScore(),
                    candidate.requestIp(),
                    candidate.fingerprint(),
                    mfaSessionService.resolveStepUpTtl()
            );
            mfaAuditService.recordBackupCodeStepUpSuccess(
                    user.getLoginId(),
                    candidate.currentScore(),
                    candidate.requiredScore(),
                    candidate.requiredScore(),
                    requestIp,
                    sessionFingerprint,
                    candidate.returnUri(),
                    remainingCodes
            );
            redirectAttributes.addFlashAttribute("operationMessage",
                    "backup code로 step-up 인증을 완료했습니다. 남은 backup code: " + remainingCodes + "개");
            return "redirect:" + candidate.returnUri();
        }

        mfaSessionService.markLoginVerified(session);
        mfaAuditService.recordBackupCodeLoginSuccess(user.getLoginId(), requestIp, sessionFingerprint, remainingCodes);
        zeroTrustService.finalizeSuccessfulLogin(
                user.getLoginId(),
                requestIp,
                request.getHeader("User-Agent")
        );
        passwordSessionService.markAuthenticationEstablished(session);
        redirectAttributes.addFlashAttribute("operationMessage",
                "backup code로 로그인 인증을 완료했습니다. 남은 backup code: " + remainingCodes + "개");
        return "redirect:/";
    }

    @GetMapping("/mfa/recovery/codes")
    public String backupCodes(Authentication authentication,
                              HttpSession session,
                              @RequestParam(name = "source", defaultValue = "setup") String source,
                              Model model) {
        User user = requireAdminUser(authentication);
        java.util.List<String> codes = mfaSessionService.getGeneratedBackupCodes(session);
        if (codes.isEmpty()) {
            return "redirect:/";
        }

        model.addAttribute("loginId", user.getLoginId());
        model.addAttribute("backupCodes", codes);
        model.addAttribute("reissued", "reissue".equalsIgnoreCase(source));
        model.addAttribute("ackReturnTo", "reissue".equalsIgnoreCase(source) ? "/mfa/recovery/status" : "/");
        return "auth/mfa_recovery_codes";
    }

    @PostMapping("/mfa/recovery/codes/ack")
    public String acknowledgeBackupCodes(HttpSession session,
                                         @RequestParam(name = "returnTo", defaultValue = "/") String returnTo) {
        mfaSessionService.clearGeneratedBackupCodes(session);
        return "redirect:" + (returnTo.startsWith("/") ? returnTo : "/");
    }

    @GetMapping("/mfa/recovery/status")
    public String backupCodeStatus(Authentication authentication,
                                   HttpSession session,
                                   Model model) {
        User user = requireAdminUser(authentication);
        if (mfaSessionService.isLoginVerificationRequired(session)) {
            return "redirect:/mfa/verify";
        }

        long remainingCodes = mfaBackupCodeService.countRemainingCodes(user.getLoginId());
        model.addAttribute("loginId", user.getLoginId());
        model.addAttribute("remainingCodes", remainingCodes);
        model.addAttribute("backupCodeExhausted", remainingCodes == 0);
        return "auth/mfa_recovery_status";
    }

    @PostMapping("/mfa/recovery/reissue")
    public String reissueBackupCodes(Authentication authentication,
                                     HttpServletRequest request,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User user = requireAdminUser(authentication);
        if (mfaSessionService.isLoginVerificationRequired(session)) {
            return "redirect:/mfa/verify";
        }

        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        long remainingCodesBefore = mfaBackupCodeService.countRemainingCodes(user.getLoginId());
        mfaAuditService.recordBackupCodeReissueStarted(
                user.getLoginId(),
                requestIp,
                sessionFingerprint,
                remainingCodesBefore
        );

        try {
            java.util.List<String> newCodes = mfaBackupCodeService.issueNewCodes(user.getLoginId());
            mfaSessionService.storeGeneratedBackupCodes(session, newCodes);
            mfaAuditService.recordBackupCodeReissueSuccess(
                    user.getLoginId(),
                    requestIp,
                    sessionFingerprint,
                    remainingCodesBefore,
                    newCodes.size()
            );
            redirectAttributes.addFlashAttribute("operationMessage",
                    "backup code를 재발급했습니다. 기존 미사용 코드는 모두 폐기되었습니다.");
            return "redirect:/mfa/recovery/codes?source=reissue";
        } catch (IllegalArgumentException ex) {
            mfaAuditService.recordBackupCodeReissueFailure(
                    user.getLoginId(),
                    requestIp,
                    sessionFingerprint,
                    remainingCodesBefore,
                    ex.getMessage()
            );
            redirectAttributes.addFlashAttribute("mfaError", ex.getMessage());
            return "redirect:/mfa/recovery/status";
        }
    }

    @PostMapping("/mfa/verify")
    public String verifyMfaCode(@RequestParam("code") String code,
                                Authentication authentication,
                                HttpServletRequest request,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = requireAdminUser(authentication);
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        boolean isStepUpFlow = mfaSessionService.isStepUpPending(session);
        MfaSessionService.StepUpCandidate candidate = isStepUpFlow ? mfaSessionService.getStepUpCandidate(session) : null;
        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            mfaAuditService.recordLoginVerifyFailure(user.getLoginId(), requestIp, sessionFingerprint,
                    "MFA가 설정되지 않았습니다.");
            redirectAttributes.addFlashAttribute("mfaError", "MFA가 설정되지 않았습니다.");
            return "redirect:/mfa/setup";
        }
        String decryptedSecret = mfaSecretCryptoService.decryptIfNeeded(user.getMfaSecret());
        if (!mfaService.verifyCode(decryptedSecret, code)) {
            if (isStepUpFlow) {
                mfaAuditService.recordStepUpFailure(
                        user.getLoginId(),
                        candidate != null ? candidate.currentScore() : null,
                        candidate != null ? candidate.requiredScore() : null,
                        requestIp,
                        sessionFingerprint,
                        candidate != null ? candidate.returnUri() : null,
                        "유효하지 않은 MFA 코드입니다."
                );
            } else {
                mfaAuditService.recordLoginVerifyFailure(user.getLoginId(), requestIp, sessionFingerprint,
                        "유효하지 않은 MFA 코드입니다.");
            }
            redirectAttributes.addFlashAttribute("mfaError", "유효하지 않은 MFA 코드입니다.");
            return "redirect:/mfa/verify";
        }

        if (isStepUpFlow) {
            if (candidate == null || !user.getLoginId().equals(candidate.loginId())) {
                mfaAuditService.recordStepUpFailure(
                        user.getLoginId(),
                        candidate != null ? candidate.currentScore() : null,
                        candidate != null ? candidate.requiredScore() : null,
                        requestIp,
                        sessionFingerprint,
                        candidate != null ? candidate.returnUri() : null,
                        "Step-up 인증 세션이 유효하지 않습니다."
                );
                redirectAttributes.addFlashAttribute("mfaError", "Step-up 인증 세션이 유효하지 않습니다.");
                return "redirect:/trust/debug";
            }

            mfaSessionService.grantStepUp(
                    session,
                    user.getLoginId(),
                    candidate.requiredScore(),
                    candidate.requestIp(),
                    candidate.fingerprint(),
                    mfaSessionService.resolveStepUpTtl()
            );
            mfaAuditService.recordStepUpSuccess(
                    user.getLoginId(),
                    candidate.currentScore(),
                    candidate.requiredScore(),
                    candidate.requiredScore(),
                    requestIp,
                    sessionFingerprint,
                    candidate.returnUri()
            );

            log.warn("[MFA-STEP-UP-SUCCESS] User: {} | Granted Score: {} | Expires In: {}m | ReturnUri: {}",
                    user.getLoginId(), candidate.requiredScore(), mfaStepUpProperties.getTtlMinutes(), candidate.returnUri());
            return "redirect:" + candidate.returnUri();
        }

        mfaSessionService.markLoginVerified(session);
        mfaAuditService.recordLoginVerifySuccess(user.getLoginId(), requestIp, sessionFingerprint);
        int finalScore = zeroTrustService.finalizeSuccessfulLogin(
                user.getLoginId(),
                requestIp,
                request.getHeader("User-Agent")
        );

        passwordSessionService.markAuthenticationEstablished(session);
        log.info("[MFA-LOGIN-SUCCESS] User: {} | Final Score: {}", user.getLoginId(), finalScore);
        return "redirect:/";
    }

    @PostMapping("/mfa/step-up/start")
    public String startStepUp(Authentication authentication,
                              HttpServletRequest request,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = requireAdminUser(authentication);
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));
        if (!user.isMfaEnabled()) {
            mfaAuditService.recordStepUpFailure(user.getLoginId(), null, null, requestIp, sessionFingerprint, null,
                    "관리자 MFA 설정이 필요합니다.");
            redirectAttributes.addFlashAttribute("mfaError", "관리자 MFA 설정이 필요합니다.");
            return "redirect:/mfa/setup";
        }

        MfaSessionService.StepUpCandidate candidate = mfaSessionService.getStepUpCandidate(session);
        if (candidate == null || !user.getLoginId().equals(candidate.loginId())) {
            mfaAuditService.recordStepUpFailure(user.getLoginId(), null, null, requestIp, sessionFingerprint, null,
                    "추가 인증 대상 요청이 없습니다.");
            redirectAttributes.addFlashAttribute("mfaError", "추가 인증 대상 요청이 없습니다. 다시 시도해주세요.");
            return "redirect:/trust/debug";
        }

        mfaSessionService.markStepUpPending(session);
        mfaAuditService.recordStepUpStarted(
                user.getLoginId(),
                candidate.currentScore(),
                candidate.requiredScore(),
                requestIp,
                sessionFingerprint,
                candidate.returnUri()
        );
        log.warn("[MFA-STEP-UP-STARTED] User: {} | Current Score: {} | Required Score: {} | ReturnUri: {}",
                user.getLoginId(), candidate.currentScore(), candidate.requiredScore(), candidate.returnUri());
        return "redirect:/mfa/verify";
    }

    private User requireAdminUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication is required");
        }

        User user = userRepository.findByLoginId(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (user.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException("Admin MFA is only available for administrators");
        }

        return user;
    }

    private String resolveOrCreatePendingSetupSecret(HttpSession session) {
        String secret = mfaSessionService.getPendingSetupSecret(session);
        if (secret == null) {
            secret = mfaService.generateSecret();
            mfaSessionService.storePendingSetupSecret(session, secret);
        }
        return secret;
    }
}
