package com.seal.seal_lab.infra.logging;

public final class MfaAuditLogFormatter {

    private MfaAuditLogFormatter() {
    }

    public static String formatSetupStarted(String loginId, String requestIp, String sessionFingerprint) {
        return """
                [MFA-AUDIT]
                Result: STARTED
                Flow: LOGIN_SETUP
                Login ID: %s
                Request IP: %s
                Session Fingerprint: %s
                """.formatted(safe(loginId), safe(requestIp), safe(sessionFingerprint));
    }

    public static String formatSetupSuccess(String loginId, String requestIp, String sessionFingerprint) {
        return """
                [MFA-AUDIT]
                Result: SUCCESS
                Flow: LOGIN_SETUP
                Login ID: %s
                Request IP: %s
                Session Fingerprint: %s
                """.formatted(safe(loginId), safe(requestIp), safe(sessionFingerprint));
    }

    public static String formatSetupFailure(String loginId, String requestIp, String sessionFingerprint, String failureReason) {
        return """
                [MFA-AUDIT]
                Result: FAILURE
                Flow: LOGIN_SETUP
                Login ID: %s
                Request IP: %s
                Session Fingerprint: %s
                Failure: %s
                """.formatted(safe(loginId), safe(requestIp), safe(sessionFingerprint), safe(failureReason));
    }

    public static String formatVerifyStarted(String loginId, String requestIp, String sessionFingerprint) {
        return """
                [MFA-AUDIT]
                Result: STARTED
                Flow: LOGIN_VERIFY
                Login ID: %s
                Request IP: %s
                Session Fingerprint: %s
                """.formatted(safe(loginId), safe(requestIp), safe(sessionFingerprint));
    }

    public static String formatVerifySuccess(String loginId, String requestIp, String sessionFingerprint) {
        return """
                [MFA-AUDIT]
                Result: SUCCESS
                Flow: LOGIN_VERIFY
                Login ID: %s
                Request IP: %s
                Session Fingerprint: %s
                """.formatted(safe(loginId), safe(requestIp), safe(sessionFingerprint));
    }

    public static String formatVerifyFailure(String loginId, String requestIp, String sessionFingerprint, String failureReason) {
        return """
                [MFA-AUDIT]
                Result: FAILURE
                Flow: LOGIN_VERIFY
                Login ID: %s
                Request IP: %s
                Session Fingerprint: %s
                Failure: %s
                """.formatted(safe(loginId), safe(requestIp), safe(sessionFingerprint), safe(failureReason));
    }

    public static String formatStepUpStarted(String loginId,
                                             Integer currentTrustScore,
                                             Integer requiredTrustScore,
                                             String requestIp,
                                             String sessionFingerprint,
                                             String returnUri) {
        return """
                [MFA-AUDIT]
                Result: STARTED
                Flow: STEP_UP
                Login ID: %s
                Current Score: %s
                Required Score: %s
                Request IP: %s
                Session Fingerprint: %s
                Return URI: %s
                """.formatted(
                safe(loginId),
                safe(currentTrustScore),
                safe(requiredTrustScore),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(returnUri)
        );
    }

    public static String formatStepUpSuccess(String loginId,
                                             Integer currentTrustScore,
                                             Integer requiredTrustScore,
                                             Integer grantedTrustScore,
                                             String requestIp,
                                             String sessionFingerprint,
                                             String returnUri) {
        return """
                [MFA-AUDIT]
                Result: SUCCESS
                Flow: STEP_UP
                Login ID: %s
                Current Score: %s
                Required Score: %s
                Granted Score: %s
                Request IP: %s
                Session Fingerprint: %s
                Return URI: %s
                """.formatted(
                safe(loginId),
                safe(currentTrustScore),
                safe(requiredTrustScore),
                safe(grantedTrustScore),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(returnUri)
        );
    }

    public static String formatStepUpFailure(String loginId,
                                             Integer currentTrustScore,
                                             Integer requiredTrustScore,
                                             String requestIp,
                                             String sessionFingerprint,
                                             String returnUri,
                                             String failureReason) {
        return """
                [MFA-AUDIT]
                Result: FAILURE
                Flow: STEP_UP
                Login ID: %s
                Current Score: %s
                Required Score: %s
                Request IP: %s
                Session Fingerprint: %s
                Return URI: %s
                Failure: %s
                """.formatted(
                safe(loginId),
                safe(currentTrustScore),
                safe(requiredTrustScore),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(returnUri),
                safe(failureReason)
        );
    }

    public static String formatBackupCodeStarted(String flow,
                                                 String loginId,
                                                 Integer currentTrustScore,
                                                 Integer requiredTrustScore,
                                                 String requestIp,
                                                 String sessionFingerprint,
                                                 String returnUri) {
        return """
                [MFA-AUDIT]
                Result: STARTED
                Flow: %s
                Login ID: %s
                Current Score: %s
                Required Score: %s
                Request IP: %s
                Session Fingerprint: %s
                Return URI: %s
                """.formatted(
                safe(flow),
                safe(loginId),
                safe(currentTrustScore),
                safe(requiredTrustScore),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(returnUri)
        );
    }

    public static String formatBackupCodeSuccess(String flow,
                                                 String loginId,
                                                 Integer currentTrustScore,
                                                 Integer requiredTrustScore,
                                                 Integer grantedTrustScore,
                                                 String requestIp,
                                                 String sessionFingerprint,
                                                 String returnUri,
                                                 Long remainingCodes) {
        return """
                [MFA-AUDIT]
                Result: SUCCESS
                Flow: %s
                Login ID: %s
                Current Score: %s
                Required Score: %s
                Granted Score: %s
                Remaining Backup Codes: %s
                Request IP: %s
                Session Fingerprint: %s
                Return URI: %s
                """.formatted(
                safe(flow),
                safe(loginId),
                safe(currentTrustScore),
                safe(requiredTrustScore),
                safe(grantedTrustScore),
                remainingCodes == null ? "-" : remainingCodes.toString(),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(returnUri)
        );
    }

    public static String formatBackupCodeFailure(String flow,
                                                 String loginId,
                                                 Integer currentTrustScore,
                                                 Integer requiredTrustScore,
                                                 String requestIp,
                                                 String sessionFingerprint,
                                                 String returnUri,
                                                 String failureReason) {
        return """
                [MFA-AUDIT]
                Result: FAILURE
                Flow: %s
                Login ID: %s
                Current Score: %s
                Required Score: %s
                Request IP: %s
                Session Fingerprint: %s
                Return URI: %s
                Failure: %s
                """.formatted(
                safe(flow),
                safe(loginId),
                safe(currentTrustScore),
                safe(requiredTrustScore),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(returnUri),
                safe(failureReason)
        );
    }

    public static String formatBackupCodeReissueStarted(String loginId,
                                                        String requestIp,
                                                        String sessionFingerprint,
                                                        Long remainingCodesBefore) {
        return """
                [MFA-AUDIT]
                Result: STARTED
                Flow: BACKUP_CODE_REISSUE
                Login ID: %s
                Remaining Backup Codes Before: %s
                Request IP: %s
                Session Fingerprint: %s
                """.formatted(
                safe(loginId),
                remainingCodesBefore == null ? "-" : remainingCodesBefore.toString(),
                safe(requestIp),
                safe(sessionFingerprint)
        );
    }

    public static String formatBackupCodeReissueSuccess(String loginId,
                                                        String requestIp,
                                                        String sessionFingerprint,
                                                        Long remainingCodesBefore,
                                                        Long remainingCodesAfter) {
        return """
                [MFA-AUDIT]
                Result: SUCCESS
                Flow: BACKUP_CODE_REISSUE
                Login ID: %s
                Remaining Backup Codes Before: %s
                Remaining Backup Codes After: %s
                Request IP: %s
                Session Fingerprint: %s
                """.formatted(
                safe(loginId),
                remainingCodesBefore == null ? "-" : remainingCodesBefore.toString(),
                remainingCodesAfter == null ? "-" : remainingCodesAfter.toString(),
                safe(requestIp),
                safe(sessionFingerprint)
        );
    }

    public static String formatBackupCodeReissueFailure(String loginId,
                                                        String requestIp,
                                                        String sessionFingerprint,
                                                        Long remainingCodesBefore,
                                                        String failureReason) {
        return """
                [MFA-AUDIT]
                Result: FAILURE
                Flow: BACKUP_CODE_REISSUE
                Login ID: %s
                Remaining Backup Codes Before: %s
                Request IP: %s
                Session Fingerprint: %s
                Failure: %s
                """.formatted(
                safe(loginId),
                remainingCodesBefore == null ? "-" : remainingCodesBefore.toString(),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(failureReason)
        );
    }

    public static String formatAdminMfaResetStarted(String operatorLoginId,
                                                    String targetLoginId,
                                                    String requestIp,
                                                    String sessionFingerprint,
                                                    String reason) {
        return """
                [MFA-AUDIT]
                Result: STARTED
                Flow: ADMIN_MFA_RESET
                Operator: %s
                Target: %s
                Request IP: %s
                Session Fingerprint: %s
                Reason: %s
                """.formatted(
                safe(operatorLoginId),
                safe(targetLoginId),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(reason)
        );
    }

    public static String formatAdminMfaResetSuccess(String operatorLoginId,
                                                    String targetLoginId,
                                                    String requestIp,
                                                    String sessionFingerprint,
                                                    String reason) {
        return """
                [MFA-AUDIT]
                Result: SUCCESS
                Flow: ADMIN_MFA_RESET
                Operator: %s
                Target: %s
                Request IP: %s
                Session Fingerprint: %s
                Reason: %s
                """.formatted(
                safe(operatorLoginId),
                safe(targetLoginId),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(reason)
        );
    }

    public static String formatAdminMfaResetFailure(String operatorLoginId,
                                                    String targetLoginId,
                                                    String requestIp,
                                                    String sessionFingerprint,
                                                    String reason,
                                                    String failureReason) {
        return """
                [MFA-AUDIT]
                Result: FAILURE
                Flow: ADMIN_MFA_RESET
                Operator: %s
                Target: %s
                Request IP: %s
                Session Fingerprint: %s
                Reason: %s
                Failure: %s
                """.formatted(
                safe(operatorLoginId),
                safe(targetLoginId),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(reason),
                safe(failureReason)
        );
    }

    public static String formatPasswordEventSuccess(String flow,
                                                    String loginId,
                                                    String requestIp,
                                                    String sessionFingerprint,
                                                    String returnUri,
                                                    String detail) {
        return """
                [MFA-AUDIT]
                Result: SUCCESS
                Flow: %s
                Login ID: %s
                Request IP: %s
                Session Fingerprint: %s
                Return URI: %s
                Detail: %s
                """.formatted(
                safe(flow),
                safe(loginId),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(returnUri),
                safe(detail)
        );
    }

    public static String formatPasswordEventFailure(String flow,
                                                    String loginId,
                                                    String requestIp,
                                                    String sessionFingerprint,
                                                    String returnUri,
                                                    String detail,
                                                    String failureReason) {
        return """
                [MFA-AUDIT]
                Result: FAILURE
                Flow: %s
                Login ID: %s
                Request IP: %s
                Session Fingerprint: %s
                Return URI: %s
                Detail: %s
                Failure: %s
                """.formatted(
                safe(flow),
                safe(loginId),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(returnUri),
                safe(detail),
                safe(failureReason)
        );
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static String safe(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
