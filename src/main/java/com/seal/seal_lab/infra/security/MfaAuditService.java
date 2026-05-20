package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.MfaAuditLog;
import com.seal.seal_lab.infra.logging.MfaAuditLogFormatter;
import com.seal.seal_lab.infra.repository.MfaAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaAuditService {

    private static final Logger MEMBER_PASSWORD_AUDIT_LOGGER =
            LoggerFactory.getLogger("com.seal.seal_lab.audit.member-password");

    private final MfaAuditLogRepository mfaAuditLogRepository;

    @Transactional
    public void recordLoginSetupStarted(String loginId, String requestIp, String sessionFingerprint) {
        save(MfaAuditLog.FlowType.LOGIN_SETUP, MfaAuditLog.ResultType.STARTED, loginId,
                null, null, null, requestIp, sessionFingerprint, null, null);
        log.info("{}", MfaAuditLogFormatter.formatSetupStarted(loginId, requestIp, sessionFingerprint));
    }

    @Transactional
    public void recordLoginSetupSuccess(String loginId, String requestIp, String sessionFingerprint) {
        save(MfaAuditLog.FlowType.LOGIN_SETUP, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, null);
        log.info("{}", MfaAuditLogFormatter.formatSetupSuccess(loginId, requestIp, sessionFingerprint));
    }

    @Transactional
    public void recordLoginSetupFailure(String loginId, String requestIp, String sessionFingerprint, String failureReason) {
        save(MfaAuditLog.FlowType.LOGIN_SETUP, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatSetupFailure(loginId, requestIp, sessionFingerprint, failureReason));
    }

    @Transactional
    public void recordLoginVerifyStarted(String loginId, String requestIp, String sessionFingerprint) {
        save(MfaAuditLog.FlowType.LOGIN_VERIFY, MfaAuditLog.ResultType.STARTED, loginId,
                null, null, null, requestIp, sessionFingerprint, null, null);
        log.info("{}", MfaAuditLogFormatter.formatVerifyStarted(loginId, requestIp, sessionFingerprint));
    }

    @Transactional
    public void recordLoginVerifySuccess(String loginId, String requestIp, String sessionFingerprint) {
        save(MfaAuditLog.FlowType.LOGIN_VERIFY, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, null);
        log.info("{}", MfaAuditLogFormatter.formatVerifySuccess(loginId, requestIp, sessionFingerprint));
    }

    @Transactional
    public void recordLoginVerifyFailure(String loginId, String requestIp, String sessionFingerprint, String failureReason) {
        save(MfaAuditLog.FlowType.LOGIN_VERIFY, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatVerifyFailure(loginId, requestIp, sessionFingerprint, failureReason));
    }

    @Transactional
    public void recordStepUpStarted(String loginId,
                                    Integer currentTrustScore,
                                    Integer requiredTrustScore,
                                    String requestIp,
                                    String sessionFingerprint,
                                    String returnUri) {
        save(MfaAuditLog.FlowType.STEP_UP, MfaAuditLog.ResultType.STARTED, loginId,
                currentTrustScore, requiredTrustScore, null, requestIp, sessionFingerprint, returnUri, null);
        log.info("{}", MfaAuditLogFormatter.formatStepUpStarted(
                loginId, currentTrustScore, requiredTrustScore, requestIp, sessionFingerprint, returnUri));
    }

    @Transactional
    public void recordStepUpSuccess(String loginId,
                                    Integer currentTrustScore,
                                    Integer requiredTrustScore,
                                    Integer grantedTrustScore,
                                    String requestIp,
                                    String sessionFingerprint,
                                    String returnUri) {
        save(MfaAuditLog.FlowType.STEP_UP, MfaAuditLog.ResultType.SUCCESS, loginId,
                currentTrustScore, requiredTrustScore, grantedTrustScore, requestIp, sessionFingerprint, returnUri, null);
        log.info("{}", MfaAuditLogFormatter.formatStepUpSuccess(
                loginId, currentTrustScore, requiredTrustScore, grantedTrustScore, requestIp, sessionFingerprint, returnUri));
    }

    @Transactional
    public void recordStepUpFailure(String loginId,
                                    Integer currentTrustScore,
                                    Integer requiredTrustScore,
                                    String requestIp,
                                    String sessionFingerprint,
                                    String returnUri,
                                    String failureReason) {
        save(MfaAuditLog.FlowType.STEP_UP, MfaAuditLog.ResultType.FAILURE, loginId,
                currentTrustScore, requiredTrustScore, null, requestIp, sessionFingerprint, returnUri, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatStepUpFailure(
                loginId, currentTrustScore, requiredTrustScore, requestIp, sessionFingerprint, returnUri, failureReason));
    }

    @Transactional
    public void recordBackupCodeLoginStarted(String loginId, String requestIp, String sessionFingerprint) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_LOGIN, MfaAuditLog.ResultType.STARTED, loginId,
                null, null, null, requestIp, sessionFingerprint, null, null);
        log.info("{}", MfaAuditLogFormatter.formatBackupCodeStarted(
                "BACKUP_CODE_LOGIN", loginId, null, null, requestIp, sessionFingerprint, null));
    }

    @Transactional
    public void recordBackupCodeLoginSuccess(String loginId,
                                             String requestIp,
                                             String sessionFingerprint,
                                             long remainingCodes) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_LOGIN, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, "remaining=" + remainingCodes);
        log.info("{}", MfaAuditLogFormatter.formatBackupCodeSuccess(
                "BACKUP_CODE_LOGIN", loginId, null, null, null, requestIp, sessionFingerprint, null, remainingCodes));
    }

    @Transactional
    public void recordBackupCodeLoginFailure(String loginId,
                                             String requestIp,
                                             String sessionFingerprint,
                                             String failureReason) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_LOGIN, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatBackupCodeFailure(
                "BACKUP_CODE_LOGIN", loginId, null, null, requestIp, sessionFingerprint, null, failureReason));
    }

    @Transactional
    public void recordBackupCodeStepUpStarted(String loginId,
                                              Integer currentTrustScore,
                                              Integer requiredTrustScore,
                                              String requestIp,
                                              String sessionFingerprint,
                                              String returnUri) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_STEP_UP, MfaAuditLog.ResultType.STARTED, loginId,
                currentTrustScore, requiredTrustScore, null, requestIp, sessionFingerprint, returnUri, null);
        log.info("{}", MfaAuditLogFormatter.formatBackupCodeStarted(
                "BACKUP_CODE_STEP_UP", loginId, currentTrustScore, requiredTrustScore, requestIp, sessionFingerprint, returnUri));
    }

    @Transactional
    public void recordBackupCodeStepUpSuccess(String loginId,
                                              Integer currentTrustScore,
                                              Integer requiredTrustScore,
                                              Integer grantedTrustScore,
                                              String requestIp,
                                              String sessionFingerprint,
                                              String returnUri,
                                              long remainingCodes) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_STEP_UP, MfaAuditLog.ResultType.SUCCESS, loginId,
                currentTrustScore, requiredTrustScore, grantedTrustScore, requestIp, sessionFingerprint, returnUri,
                "remaining=" + remainingCodes);
        log.info("{}", MfaAuditLogFormatter.formatBackupCodeSuccess(
                "BACKUP_CODE_STEP_UP", loginId, currentTrustScore, requiredTrustScore, grantedTrustScore,
                requestIp, sessionFingerprint, returnUri, remainingCodes));
    }

    @Transactional
    public void recordBackupCodeStepUpFailure(String loginId,
                                              Integer currentTrustScore,
                                              Integer requiredTrustScore,
                                              String requestIp,
                                              String sessionFingerprint,
                                              String returnUri,
                                              String failureReason) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_STEP_UP, MfaAuditLog.ResultType.FAILURE, loginId,
                currentTrustScore, requiredTrustScore, null, requestIp, sessionFingerprint, returnUri, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatBackupCodeFailure(
                "BACKUP_CODE_STEP_UP", loginId, currentTrustScore, requiredTrustScore, requestIp, sessionFingerprint, returnUri, failureReason));
    }

    @Transactional
    public void recordBackupCodeReissueStarted(String loginId,
                                               String requestIp,
                                               String sessionFingerprint,
                                               long remainingCodesBefore) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_REISSUE, MfaAuditLog.ResultType.STARTED, loginId,
                null, null, null, requestIp, sessionFingerprint, null, "remainingBefore=" + remainingCodesBefore);
        log.info("{}", MfaAuditLogFormatter.formatBackupCodeReissueStarted(
                loginId, requestIp, sessionFingerprint, remainingCodesBefore));
    }

    @Transactional
    public void recordBackupCodeReissueSuccess(String loginId,
                                               String requestIp,
                                               String sessionFingerprint,
                                               long remainingCodesBefore,
                                               long remainingCodesAfter) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_REISSUE, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null,
                "remainingBefore=" + remainingCodesBefore + ",remainingAfter=" + remainingCodesAfter);
        log.info("{}", MfaAuditLogFormatter.formatBackupCodeReissueSuccess(
                loginId, requestIp, sessionFingerprint, remainingCodesBefore, remainingCodesAfter));
    }

    @Transactional
    public void recordBackupCodeReissueFailure(String loginId,
                                               String requestIp,
                                               String sessionFingerprint,
                                               long remainingCodesBefore,
                                               String failureReason) {
        save(MfaAuditLog.FlowType.BACKUP_CODE_REISSUE, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatBackupCodeReissueFailure(
                loginId, requestIp, sessionFingerprint, remainingCodesBefore, failureReason));
    }

    @Transactional
    public void recordAdminMfaResetStarted(String operatorLoginId,
                                           String targetLoginId,
                                           String requestIp,
                                           String sessionFingerprint,
                                           String reason) {
        save(MfaAuditLog.FlowType.ADMIN_MFA_RESET, MfaAuditLog.ResultType.STARTED, operatorLoginId,
                null, null, null, requestIp, sessionFingerprint, null,
                "target=" + targetLoginId + ",reason=" + reason);
        log.info("{}", MfaAuditLogFormatter.formatAdminMfaResetStarted(
                operatorLoginId, targetLoginId, requestIp, sessionFingerprint, reason));
    }

    @Transactional
    public void recordAdminMfaResetSuccess(String operatorLoginId,
                                           String targetLoginId,
                                           String requestIp,
                                           String sessionFingerprint,
                                           String reason) {
        save(MfaAuditLog.FlowType.ADMIN_MFA_RESET, MfaAuditLog.ResultType.SUCCESS, operatorLoginId,
                null, null, null, requestIp, sessionFingerprint, null,
                "target=" + targetLoginId + ",reason=" + reason + ",backupCodesCleared=true");
        log.warn("{}", MfaAuditLogFormatter.formatAdminMfaResetSuccess(
                operatorLoginId, targetLoginId, requestIp, sessionFingerprint, reason));
    }

    @Transactional
    public void recordAdminMfaResetFailure(String operatorLoginId,
                                           String targetLoginId,
                                           String requestIp,
                                           String sessionFingerprint,
                                           String reason,
                                           String failureReason) {
        save(MfaAuditLog.FlowType.ADMIN_MFA_RESET, MfaAuditLog.ResultType.FAILURE, operatorLoginId,
                null, null, null, requestIp, sessionFingerprint, null,
                "target=" + targetLoginId + ",reason=" + reason + ",failure=" + failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatAdminMfaResetFailure(
                operatorLoginId, targetLoginId, requestIp, sessionFingerprint, reason, failureReason));
    }

    @Transactional
    public void recordAdminPasswordChangeSuccess(String loginId, String requestIp, String sessionFingerprint) {
        save(MfaAuditLog.FlowType.ADMIN_PASSWORD_CHANGE, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, "self-service password rotation");
        log.warn("{}", MfaAuditLogFormatter.formatPasswordEventSuccess(
                "ADMIN_PASSWORD_CHANGE", loginId, requestIp, sessionFingerprint, null, "self-service password rotation"));
    }

    @Transactional
    public void recordAdminPasswordChangeFailure(String loginId,
                                                 String requestIp,
                                                 String sessionFingerprint,
                                                 String failureReason) {
        save(MfaAuditLog.FlowType.ADMIN_PASSWORD_CHANGE, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatPasswordEventFailure(
                "ADMIN_PASSWORD_CHANGE", loginId, requestIp, sessionFingerprint, null, null, failureReason));
    }

    @Transactional
    public void recordMemberPasswordChangeSuccess(String loginId, String requestIp, String sessionFingerprint) {
        save(MfaAuditLog.FlowType.MEMBER_PASSWORD_CHANGE, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, "self-service password rotation");
        MEMBER_PASSWORD_AUDIT_LOGGER.info("{}", MfaAuditLogFormatter.formatPasswordEventSuccess(
                "MEMBER_PASSWORD_CHANGE", loginId, requestIp, sessionFingerprint, null, "self-service password rotation"));
    }

    @Transactional
    public void recordMemberPasswordChangeFailure(String loginId,
                                                  String requestIp,
                                                  String sessionFingerprint,
                                                  String failureReason) {
        save(MfaAuditLog.FlowType.MEMBER_PASSWORD_CHANGE, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        MEMBER_PASSWORD_AUDIT_LOGGER.warn("{}", MfaAuditLogFormatter.formatPasswordEventFailure(
                "MEMBER_PASSWORD_CHANGE", loginId, requestIp, sessionFingerprint, null, null, failureReason));
    }

    @Transactional
    public void recordAdminPasswordResetIssueSuccess(String loginId,
                                                     String requestIp,
                                                     String sessionFingerprint,
                                                     LocalDateTime expiresAt) {
        save(MfaAuditLog.FlowType.ADMIN_PASSWORD_RESET_ISSUE, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, "expiresAt=" + expiresAt);
        log.warn("{}", MfaAuditLogFormatter.formatPasswordEventSuccess(
                "ADMIN_PASSWORD_RESET_ISSUE", loginId, requestIp, sessionFingerprint, null, "expiresAt=" + expiresAt));
    }

    @Transactional
    public void recordAdminPasswordResetIssueFailure(String loginId,
                                                     String requestIp,
                                                     String sessionFingerprint,
                                                     String failureReason) {
        save(MfaAuditLog.FlowType.ADMIN_PASSWORD_RESET_ISSUE, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatPasswordEventFailure(
                "ADMIN_PASSWORD_RESET_ISSUE", loginId, requestIp, sessionFingerprint, null, null, failureReason));
    }

    @Transactional
    public void recordMemberPasswordResetIssueSuccess(String loginId,
                                                      String requestIp,
                                                      String sessionFingerprint,
                                                      LocalDateTime expiresAt) {
        save(MfaAuditLog.FlowType.MEMBER_PASSWORD_RESET_ISSUE, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, "expiresAt=" + expiresAt);
        MEMBER_PASSWORD_AUDIT_LOGGER.info("{}", MfaAuditLogFormatter.formatPasswordEventSuccess(
                "MEMBER_PASSWORD_RESET_ISSUE", loginId, requestIp, sessionFingerprint, null, "expiresAt=" + expiresAt));
    }

    @Transactional
    public void recordMemberPasswordResetIssueFailure(String loginId,
                                                      String requestIp,
                                                      String sessionFingerprint,
                                                      String failureReason) {
        save(MfaAuditLog.FlowType.MEMBER_PASSWORD_RESET_ISSUE, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        MEMBER_PASSWORD_AUDIT_LOGGER.warn("{}", MfaAuditLogFormatter.formatPasswordEventFailure(
                "MEMBER_PASSWORD_RESET_ISSUE", loginId, requestIp, sessionFingerprint, null, null, failureReason));
    }

    @Transactional
    public void recordAdminPasswordResetCompleteSuccess(String loginId,
                                                        String requestIp,
                                                        String sessionFingerprint) {
        save(MfaAuditLog.FlowType.ADMIN_PASSWORD_RESET_COMPLETE, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, "backup code verified");
        log.warn("{}", MfaAuditLogFormatter.formatPasswordEventSuccess(
                "ADMIN_PASSWORD_RESET_COMPLETE", loginId, requestIp, sessionFingerprint, null, "backup code verified"));
    }

    @Transactional
    public void recordAdminPasswordResetCompleteFailure(String loginId,
                                                        String requestIp,
                                                        String sessionFingerprint,
                                                        String failureReason) {
        save(MfaAuditLog.FlowType.ADMIN_PASSWORD_RESET_COMPLETE, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        log.warn("{}", MfaAuditLogFormatter.formatPasswordEventFailure(
                "ADMIN_PASSWORD_RESET_COMPLETE", loginId, requestIp, sessionFingerprint, null, null, failureReason));
    }

    @Transactional
    public void recordMemberPasswordResetCompleteSuccess(String loginId,
                                                         String requestIp,
                                                         String sessionFingerprint) {
        save(MfaAuditLog.FlowType.MEMBER_PASSWORD_RESET_COMPLETE, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, null, "member password reset completed");
        MEMBER_PASSWORD_AUDIT_LOGGER.info("{}", MfaAuditLogFormatter.formatPasswordEventSuccess(
                "MEMBER_PASSWORD_RESET_COMPLETE", loginId, requestIp, sessionFingerprint, null, "member password reset completed"));
    }

    @Transactional
    public void recordMemberPasswordResetCompleteFailure(String loginId,
                                                         String requestIp,
                                                         String sessionFingerprint,
                                                         String failureReason) {
        save(MfaAuditLog.FlowType.MEMBER_PASSWORD_RESET_COMPLETE, MfaAuditLog.ResultType.FAILURE, loginId,
                null, null, null, requestIp, sessionFingerprint, null, failureReason);
        MEMBER_PASSWORD_AUDIT_LOGGER.warn("{}", MfaAuditLogFormatter.formatPasswordEventFailure(
                "MEMBER_PASSWORD_RESET_COMPLETE", loginId, requestIp, sessionFingerprint, null, null, failureReason));
    }

    @Transactional
    public void recordPasswordSessionRevoked(String loginId,
                                             String requestIp,
                                             String sessionFingerprint,
                                             String returnUri) {
        save(MfaAuditLog.FlowType.PASSWORD_SESSION_REVOKE, MfaAuditLog.ResultType.SUCCESS, loginId,
                null, null, null, requestIp, sessionFingerprint, returnUri, "passwordChangedAt invalidated this session");
        log.warn("{}", MfaAuditLogFormatter.formatPasswordEventSuccess(
                "PASSWORD_SESSION_REVOKE", loginId, requestIp, sessionFingerprint, returnUri,
                "passwordChangedAt invalidated this session"));
    }

    @Transactional(readOnly = true)
    public List<MfaAuditLog> searchAuditLogs(String loginId,
                                             String flowTypeFilter,
                                             String resultTypeFilter,
                                             LocalDate dateFrom,
                                             LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }

        return mfaAuditLogRepository.searchAuditLogs(
                normalizeFilter(loginId),
                normalizeFlowTypeFilter(flowTypeFilter),
                normalizeResultTypeFilter(resultTypeFilter),
                dateFrom != null ? dateFrom.atStartOfDay() : null,
                dateTo != null ? dateTo.atTime(23, 59, 59) : null,
                PageRequest.of(0, 50)
        );
    }

    private void save(MfaAuditLog.FlowType flowType,
                      MfaAuditLog.ResultType resultType,
                      String loginId,
                      Integer currentTrustScore,
                      Integer requiredTrustScore,
                      Integer grantedTrustScore,
                      String requestIp,
                      String sessionFingerprint,
                      String returnUri,
                      String detailMessage) {
        mfaAuditLogRepository.save(MfaAuditLog.builder()
                .loginId(loginId)
                .flowType(flowType)
                .resultType(resultType)
                .currentTrustScore(currentTrustScore)
                .requiredTrustScore(requiredTrustScore)
                .grantedTrustScore(grantedTrustScore)
                .requestIp(requestIp)
                .sessionFingerprint(sessionFingerprint)
                .returnUri(returnUri)
                .detailMessage(detailMessage)
                .build());
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private MfaAuditLog.FlowType normalizeFlowTypeFilter(String flowTypeFilter) {
        String normalized = normalizeFilter(flowTypeFilter);
        if (normalized == null) {
            return null;
        }
        try {
            return MfaAuditLog.FlowType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 flowType 필터입니다.");
        }
    }

    private MfaAuditLog.ResultType normalizeResultTypeFilter(String resultTypeFilter) {
        String normalized = normalizeFilter(resultTypeFilter);
        if (normalized == null) {
            return null;
        }
        try {
            return MfaAuditLog.ResultType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 resultType 필터입니다.");
        }
    }
}
