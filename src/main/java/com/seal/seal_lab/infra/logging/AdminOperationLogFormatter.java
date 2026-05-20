package com.seal.seal_lab.infra.logging;

public final class AdminOperationLogFormatter {

    private AdminOperationLogFormatter() {
    }

    public static String formatManualOverrideSuccess(String operator,
                                                     String target,
                                                     int previousScore,
                                                     int appliedScore,
                                                     String clearContext,
                                                     String requestIp,
                                                     String sessionFingerprint,
                                                     String operationReason) {
        return """
                [ZTA-ADMIN]
                Result: SUCCESS
                Action: MANUAL_OVERRIDE
                Operator: %s
                Target: %s
                Score Change: %s
                Context Reset: %s
                Request IP: %s
                Session Fingerprint: %s
                Reason: %s
                """.formatted(
                safe(operator),
                safe(target),
                formatScoreChange(previousScore, appliedScore),
                safe(clearContext),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(operationReason)
        );
    }

    public static String formatTestBaselineSuccess(String operator,
                                                   String target,
                                                   int previousScore,
                                                   String requestIp,
                                                   String sessionFingerprint,
                                                   String fingerprint,
                                                   String operationReason) {
        return """
                [ZTA-ADMIN]
                Result: SUCCESS
                Action: TEST_BASELINE_RESET
                Operator: %s
                Target: %s
                Score Change: %s
                Context Reset: true
                Request IP: %s
                Session Fingerprint: %s
                Fingerprint: %s
                Reason: %s
                """.formatted(
                safe(operator),
                safe(target),
                formatScoreChange(previousScore, 100),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(fingerprint),
                safe(operationReason)
        );
    }

    public static String formatFailure(String action,
                                       String operator,
                                       String target,
                                       String failureReason,
                                       String operationReason,
                                       String requestedScore,
                                       String clearContext,
                                       String requestIp,
                                       String sessionFingerprint) {
        return """
                [ZTA-ADMIN]
                Result: FAILURE
                Action: %s
                Operator: %s
                Target: %s
                Requested Score: %s
                Context Reset: %s
                Request IP: %s
                Session Fingerprint: %s
                Reason: %s
                Failure: %s
                """.formatted(
                safe(action),
                safe(operator),
                safe(target),
                safe(requestedScore),
                safe(clearContext),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(operationReason),
                safe(failureReason)
        );
    }

    public static String formatTrustDenied(String action,
                                           String operator,
                                           String target,
                                           String currentScore,
                                           String requiredScore,
                                           String operationReason,
                                           String requestedScore,
                                           String clearContext,
                                           String requestIp,
                                           String sessionFingerprint,
                                           String failureReason) {
        return """
                [ZTA-ADMIN]
                Result: FAILURE
                Action: %s
                Operator: %s
                Target: %s
                Current Score: %s
                Required Score: %s
                Requested Score: %s
                Context Reset: %s
                Request IP: %s
                Session Fingerprint: %s
                Reason: %s
                Failure: %s
                """.formatted(
                safe(action),
                safe(operator),
                safe(target),
                safe(currentScore),
                safe(requiredScore),
                safe(requestedScore),
                safe(clearContext),
                safe(requestIp),
                safe(sessionFingerprint),
                safe(operationReason),
                safe(failureReason)
        );
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static String formatScoreChange(int previousScore, int appliedScore) {
        int delta = appliedScore - previousScore;
        String deltaText = delta > 0 ? "+" + delta : String.valueOf(delta);
        return previousScore + " -> " + appliedScore + " (" + deltaText + ")";
    }
}
