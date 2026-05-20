package com.seal.seal_lab.infra.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminOperationLogFormatterTests {

    @Test
    void formatsManualOverrideSuccessAsReadableMultilineBlock() {
        String message = AdminOperationLogFormatter.formatManualOverrideSuccess(
                "admin",
                "jspark",
                70,
                95,
                "true",
                "203.0.113.10",
                "fp:firefox|windows|desktop",
                "실검증 전 점수 복구"
        );

        assertThat(message).contains("[ZTA-ADMIN]");
        assertThat(message).contains("Result: SUCCESS");
        assertThat(message).contains("Action: MANUAL_OVERRIDE");
        assertThat(message).contains("Operator: admin");
        assertThat(message).contains("Target: jspark");
        assertThat(message).contains("Score Change: 70 -> 95 (+25)");
        assertThat(message).contains("Request IP: 203.0.113.10");
        assertThat(message).contains("Session Fingerprint: fp:firefox|windows|desktop");
        assertThat(message).contains("Reason: 실검증 전 점수 복구");
    }

    @Test
    void formatsTestBaselineSuccessWithVisibleScoreDelta() {
        String message = AdminOperationLogFormatter.formatTestBaselineSuccess(
                "admin",
                "jspark",
                82,
                "203.0.113.10",
                "fp:chrome|windows|desktop",
                "fp:chrome|windows|desktop",
                "브라우저 테스트 초기화"
        );

        assertThat(message).contains("Action: TEST_BASELINE_RESET");
        assertThat(message).contains("Score Change: 82 -> 100 (+18)");
        assertThat(message).contains("Request IP: 203.0.113.10");
        assertThat(message).contains("Session Fingerprint: fp:chrome|windows|desktop");
        assertThat(message).contains("Fingerprint: fp:chrome|windows|desktop");
    }

    @Test
    void formatsFailureWithFallbackForMissingValues() {
        String message = AdminOperationLogFormatter.formatFailure(
                "TEST_BASELINE_RESET",
                "admin",
                null,
                "ROLE_ADMIN required",
                "",
                null,
                null,
                null,
                null
        );

        assertThat(message).contains("Result: FAILURE");
        assertThat(message).contains("Action: TEST_BASELINE_RESET");
        assertThat(message).contains("Target: -");
        assertThat(message).contains("Requested Score: -");
        assertThat(message).contains("Context Reset: -");
        assertThat(message).contains("Request IP: -");
        assertThat(message).contains("Session Fingerprint: -");
        assertThat(message).contains("Reason: -");
        assertThat(message).contains("Failure: ROLE_ADMIN required");
    }

    @Test
    void formatsTrustDeniedWithCurrentAndRequiredScores() {
        String message = AdminOperationLogFormatter.formatTrustDenied(
                "MANUAL_OVERRIDE",
                "admin",
                "jspark",
                "75",
                "95",
                "운영 점수 복구",
                "100",
                "true",
                "203.0.113.10",
                "fp:firefox|windows|desktop",
                "현재 신뢰 점수가 요구 점수보다 낮습니다."
        );

        assertThat(message).contains("Current Score: 75");
        assertThat(message).contains("Required Score: 95");
        assertThat(message).contains("Requested Score: 100");
        assertThat(message).contains("Request IP: 203.0.113.10");
        assertThat(message).contains("Session Fingerprint: fp:firefox|windows|desktop");
        assertThat(message).contains("Failure: 현재 신뢰 점수가 요구 점수보다 낮습니다.");
    }
}
