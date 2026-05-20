package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.api.dto.TrustDebugView;
import com.seal.seal_lab.core.annotation.ZeroTrust;
import com.seal.seal_lab.core.entity.MfaAuditLog;
import com.seal.seal_lab.core.service.ZeroTrustService;
import com.seal.seal_lab.infra.logging.AdminOperationLogFormatter;
import com.seal.seal_lab.infra.security.AdminMfaResetService;
import com.seal.seal_lab.infra.security.DeviceFingerprintResolver;
import com.seal.seal_lab.infra.security.MfaAuditService;
import com.seal.seal_lab.infra.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TrustDebugController {

    private final ZeroTrustService zeroTrustService;
    private final ClientIpResolver clientIpResolver;
    private final AdminMfaResetService adminMfaResetService;
    private final MfaAuditService mfaAuditService;

    @GetMapping("/trust/debug")
    public String trustDebug(@RequestParam(value = "loginId", required = false) String loginId,
                             Authentication authentication,
                             HttpServletRequest request,
                             Model model) {
        String currentLoginId = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        TrustDebugView selfStatus = zeroTrustService.inspectCurrentTrustStatus(
                currentLoginId,
                clientIpResolver.resolve(request),
                request.getHeader("User-Agent")
        );

        model.addAttribute("selfStatus", selfStatus);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("lookupLoginId", loginId);

        if (isAdmin && loginId != null && !loginId.isBlank()) {
            try {
                model.addAttribute("lookupStatus", zeroTrustService.inspectStoredTrustStatus(loginId.trim()));
            } catch (RuntimeException e) {
                model.addAttribute("lookupError", "해당 loginId의 사용자 정보를 찾을 수 없습니다.");
            }
        }

        return "trust/debug";
    }

    @GetMapping("/admin/trust/audit")
    @ZeroTrust(requiredScore = 90)
    public String trustAudit(@RequestParam(value = "operator", required = false) String operator,
                             @RequestParam(value = "target", required = false) String target,
                             @RequestParam(value = "actionType", required = false) String actionType,
                             @RequestParam(value = "contextReset", required = false) String contextReset,
                             @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                             @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                             Model model) {
        try {
            model.addAttribute("auditLogs", zeroTrustService.searchTrustOverrideLogs(
                    operator, target, actionType, contextReset, dateFrom, dateTo));
        } catch (IllegalArgumentException e) {
            model.addAttribute("auditLogs", java.util.List.of());
            model.addAttribute("auditFilterError", e.getMessage());
        }

        model.addAttribute("operatorFilter", operator);
        model.addAttribute("targetFilter", target);
        model.addAttribute("actionTypeFilter", actionType);
        model.addAttribute("contextResetFilter", contextReset);
        model.addAttribute("dateFromFilter", dateFrom);
        model.addAttribute("dateToFilter", dateTo);
        return "trust/audit";
    }

    @GetMapping("/admin/security/audit")
    @ZeroTrust(requiredScore = 90)
    public String securityAudit(@RequestParam(value = "loginId", required = false) String loginId,
                                @RequestParam(value = "flowType", required = false) String flowType,
                                @RequestParam(value = "resultType", required = false) String resultType,
                                @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                Model model) {
        try {
            model.addAttribute("auditLogs", mfaAuditService.searchAuditLogs(
                    loginId, flowType, resultType, dateFrom, dateTo));
        } catch (IllegalArgumentException e) {
            model.addAttribute("auditLogs", java.util.List.of());
            model.addAttribute("auditFilterError", e.getMessage());
        }

        model.addAttribute("loginIdFilter", loginId);
        model.addAttribute("flowTypeFilter", flowType);
        model.addAttribute("resultTypeFilter", resultType);
        model.addAttribute("dateFromFilter", dateFrom);
        model.addAttribute("dateToFilter", dateTo);
        model.addAttribute("flowTypes", MfaAuditLog.FlowType.values());
        model.addAttribute("resultTypes", MfaAuditLog.ResultType.values());
        return "security/audit";
    }

    @PostMapping("/admin/trust/override")
    @ZeroTrust(requiredScore = 95)
    public String overrideTrustState(@RequestParam("loginId") String loginId,
                                     @RequestParam("trustScore") int trustScore,
                                     @RequestParam(value = "clearContext", defaultValue = "false") boolean clearContext,
                                     @RequestParam("reason") String reason,
                                     HttpServletRequest request,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        String trimmedLoginId = loginId == null ? "" : loginId.trim();
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));

        try {
            zeroTrustService.overrideTrustState(trimmedLoginId, trustScore, clearContext, reason, requestIp, sessionFingerprint, authentication.getName());

            String successMessage = "'" + trimmedLoginId + "' 계정 점수를 " + trustScore + "점으로 적용했습니다.";
            if (clearContext) {
                successMessage += " 저장된 위치와 fingerprint 컨텍스트도 초기화했습니다.";
            } else {
                successMessage += " 저장된 위치와 fingerprint 컨텍스트는 유지했습니다.";
            }

            redirectAttributes.addFlashAttribute("operationMessage", successMessage);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            log.warn("{}", AdminOperationLogFormatter.formatFailure(
                    "MANUAL_OVERRIDE",
                    authentication.getName(),
                    trimmedLoginId,
                    e.getMessage(),
                    reason,
                    String.valueOf(trustScore),
                    String.valueOf(clearContext),
                    requestIp,
                    sessionFingerprint
            ));
            redirectAttributes.addFlashAttribute("operationError", e.getMessage());
        }

        if (!trimmedLoginId.isBlank()) {
            return "redirect:/trust/debug?loginId=" + trimmedLoginId;
        }
        return "redirect:/trust/debug";
    }

    @PostMapping("/admin/trust/test-baseline")
    @ZeroTrust(requiredScore = 95)
    public String applyTestBaseline(@RequestParam("loginId") String loginId,
                                    @RequestParam("reason") String reason,
                                    HttpServletRequest request,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        String trimmedLoginId = loginId == null ? "" : loginId.trim();
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));

        try {
            zeroTrustService.resetTrustStateToTestBaseline(trimmedLoginId, reason, requestIp, sessionFingerprint, authentication.getName());
            redirectAttributes.addFlashAttribute("operationMessage",
                    "'" + trimmedLoginId + "' 계정을 테스트 기준선으로 초기화했습니다. 100점, Windows 데스크톱 Chrome fingerprint, 위치/시간 초기화가 적용됩니다.");
        } catch (IllegalArgumentException | NoSuchElementException e) {
            log.warn("{}", AdminOperationLogFormatter.formatFailure(
                    "TEST_BASELINE_RESET",
                    authentication.getName(),
                    trimmedLoginId,
                    e.getMessage(),
                    reason,
                    null,
                    "true",
                    requestIp,
                    sessionFingerprint
            ));
            redirectAttributes.addFlashAttribute("operationError", e.getMessage());
        }

        if (!trimmedLoginId.isBlank()) {
            return "redirect:/trust/debug?loginId=" + trimmedLoginId;
        }
        return "redirect:/trust/debug";
    }

    @PostMapping("/admin/mfa/reset")
    @ZeroTrust(requiredScore = 95)
    public String resetAdminMfa(@RequestParam("loginId") String loginId,
                                @RequestParam("reason") String reason,
                                HttpServletRequest request,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String trimmedLoginId = loginId == null ? "" : loginId.trim();
        String trimmedReason = reason == null ? "" : reason.trim();
        String requestIp = clientIpResolver.resolve(request);
        String sessionFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(request.getHeader("User-Agent"));

        try {
            if (trimmedReason.isBlank()) {
                throw new IllegalArgumentException("운영자 MFA reset 사유는 비워둘 수 없습니다.");
            }

            mfaAuditService.recordAdminMfaResetStarted(
                    authentication.getName(),
                    trimmedLoginId,
                    requestIp,
                    sessionFingerprint,
                    trimmedReason
            );
            adminMfaResetService.resetAdminMfa(trimmedLoginId);
            mfaAuditService.recordAdminMfaResetSuccess(
                    authentication.getName(),
                    trimmedLoginId,
                    requestIp,
                    sessionFingerprint,
                    trimmedReason
            );
            redirectAttributes.addFlashAttribute("operationMessage",
                    "'" + trimmedLoginId + "' 계정의 MFA를 초기화했습니다. 기존 backup code도 함께 폐기되며, 다음 요청부터 다시 /mfa/setup 이 필요합니다.");
        } catch (IllegalArgumentException | NoSuchElementException e) {
            mfaAuditService.recordAdminMfaResetFailure(
                    authentication.getName(),
                    trimmedLoginId,
                    requestIp,
                    sessionFingerprint,
                    trimmedReason,
                    e.getMessage()
            );
            redirectAttributes.addFlashAttribute("operationError", e.getMessage());
        }

        if (!trimmedLoginId.isBlank()) {
            return "redirect:/trust/debug?loginId=" + trimmedLoginId;
        }
        return "redirect:/trust/debug";
    }
}
