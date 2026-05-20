package com.seal.seal_lab.infra.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MfaAuditLogFormatterTests {

    @Test
    void formatsStepUpSuccessAsReadableMultilineBlock() {
        String message = MfaAuditLogFormatter.formatStepUpSuccess(
                "admin",
                75,
                90,
                90,
                "203.0.113.10",
                "fp:firefox|windows|desktop",
                "/admin/users/member/edit"
        );

        assertThat(message).contains("[MFA-AUDIT]");
        assertThat(message).contains("Result: SUCCESS");
        assertThat(message).contains("Flow: STEP_UP");
        assertThat(message).contains("Current Score: 75");
        assertThat(message).contains("Required Score: 90");
        assertThat(message).contains("Granted Score: 90");
        assertThat(message).contains("Request IP: 203.0.113.10");
        assertThat(message).contains("Session Fingerprint: fp:firefox|windows|desktop");
        assertThat(message).contains("Return URI: /admin/users/member/edit");
    }

    @Test
    void formatsVerifyFailureWithFallbackValues() {
        String message = MfaAuditLogFormatter.formatVerifyFailure("admin", null, null, "유효하지 않은 MFA 코드입니다.");

        assertThat(message).contains("Result: FAILURE");
        assertThat(message).contains("Flow: LOGIN_VERIFY");
        assertThat(message).contains("Request IP: -");
        assertThat(message).contains("Session Fingerprint: -");
        assertThat(message).contains("Failure: 유효하지 않은 MFA 코드입니다.");
    }

    @Test
    void formatsBackupCodeReissueSuccessWithBeforeAfterCounts() {
        String message = MfaAuditLogFormatter.formatBackupCodeReissueSuccess(
                "admin",
                "203.0.113.10",
                "fp:chrome|windows|desktop",
                0L,
                8L
        );

        assertThat(message).contains("Flow: BACKUP_CODE_REISSUE");
        assertThat(message).contains("Result: SUCCESS");
        assertThat(message).contains("Remaining Backup Codes Before: 0");
        assertThat(message).contains("Remaining Backup Codes After: 8");
        assertThat(message).contains("Request IP: 203.0.113.10");
        assertThat(message).contains("Session Fingerprint: fp:chrome|windows|desktop");
    }

    @Test
    void formatsAdminMfaResetSuccessWithOperatorAndTarget() {
        String message = MfaAuditLogFormatter.formatAdminMfaResetSuccess(
                "operator-admin",
                "target-admin",
                "203.0.113.10",
                "fp:chrome|windows|desktop",
                "backup code 소진 및 기기 분실"
        );

        assertThat(message).contains("Flow: ADMIN_MFA_RESET");
        assertThat(message).contains("Result: SUCCESS");
        assertThat(message).contains("Operator: operator-admin");
        assertThat(message).contains("Target: target-admin");
        assertThat(message).contains("Reason: backup code 소진 및 기기 분실");
    }

    @Test
    void formatsPasswordEventSuccessWithDetailAndReturnUri() {
        String message = MfaAuditLogFormatter.formatPasswordEventSuccess(
                "PASSWORD_SESSION_REVOKE",
                "member",
                "203.0.113.25",
                "fp:chrome|windows|desktop",
                "/member/security/password",
                "passwordChangedAt invalidated this session"
        );

        assertThat(message).contains("Flow: PASSWORD_SESSION_REVOKE");
        assertThat(message).contains("Result: SUCCESS");
        assertThat(message).contains("Login ID: member");
        assertThat(message).contains("Request IP: 203.0.113.25");
        assertThat(message).contains("Session Fingerprint: fp:chrome|windows|desktop");
        assertThat(message).contains("Return URI: /member/security/password");
        assertThat(message).contains("Detail: passwordChangedAt invalidated this session");
    }
}
