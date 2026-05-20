package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.infra.config.MfaStepUpProperties;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MfaSessionService {

    private static final String LOGIN_VERIFIED = "mfa.loginVerified";
    private static final String LOGIN_SETUP_REQUIRED = "mfa.loginSetupRequired";
    private static final String LOGIN_ID = "mfa.loginId";
    private static final String PENDING_SETUP_SECRET = "mfa.pendingSetupSecret";
    private static final String STEP_UP_PENDING = "mfa.stepUpPending";
    private static final String STEP_UP_CANDIDATE = "mfa.stepUpCandidate";
    private static final String STEP_UP_GRANT = "mfa.stepUpGrant";
    private static final String GENERATED_BACKUP_CODES = "mfa.generatedBackupCodes";

    private final MfaStepUpProperties mfaStepUpProperties;

    public void beginLoginVerification(HttpSession session, String loginId, boolean setupRequired) {
        session.setAttribute(LOGIN_VERIFIED, Boolean.FALSE);
        session.setAttribute(LOGIN_SETUP_REQUIRED, setupRequired);
        session.setAttribute(LOGIN_ID, loginId);
        clearStepUp(session);
    }

    public boolean isLoginVerificationRequired(HttpSession session) {
        return Boolean.FALSE.equals(session.getAttribute(LOGIN_VERIFIED));
    }

    public boolean isSetupRequired(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(LOGIN_SETUP_REQUIRED));
    }

    public void markLoginVerified(HttpSession session) {
        session.setAttribute(LOGIN_VERIFIED, Boolean.TRUE);
        session.removeAttribute(LOGIN_SETUP_REQUIRED);
        session.removeAttribute(PENDING_SETUP_SECRET);
    }

    public String getLoginId(HttpSession session) {
        Object value = session.getAttribute(LOGIN_ID);
        return value instanceof String ? (String) value : null;
    }

    public void clearLoginVerification(HttpSession session) {
        session.removeAttribute(LOGIN_VERIFIED);
        session.removeAttribute(LOGIN_SETUP_REQUIRED);
        session.removeAttribute(LOGIN_ID);
        session.removeAttribute(PENDING_SETUP_SECRET);
        session.removeAttribute(GENERATED_BACKUP_CODES);
    }

    public void storePendingSetupSecret(HttpSession session, String secret) {
        session.setAttribute(PENDING_SETUP_SECRET, secret);
    }

    public String getPendingSetupSecret(HttpSession session) {
        Object value = session.getAttribute(PENDING_SETUP_SECRET);
        return value instanceof String ? (String) value : null;
    }

    public void storeStepUpCandidate(HttpSession session,
                                     String loginId,
                                     int currentScore,
                                     int requiredScore,
                                     String returnUri,
                                     String requestIp,
                                     String fingerprint) {
        session.setAttribute(STEP_UP_CANDIDATE, new StepUpCandidate(
                loginId,
                currentScore,
                requiredScore,
                returnUri,
                requestIp,
                fingerprint,
                LocalDateTime.now()
        ));
    }

    public StepUpCandidate getStepUpCandidate(HttpSession session) {
        Object value = session.getAttribute(STEP_UP_CANDIDATE);
        return value instanceof StepUpCandidate candidate ? candidate : null;
    }

    public void markStepUpPending(HttpSession session) {
        session.setAttribute(STEP_UP_PENDING, Boolean.TRUE);
    }

    public boolean isStepUpPending(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(STEP_UP_PENDING));
    }

    public void grantStepUp(HttpSession session,
                            String loginId,
                            int grantedScore,
                            String requestIp,
                            String fingerprint,
                            Duration ttl) {
        session.setAttribute(STEP_UP_GRANT, new StepUpGrant(
                loginId,
                grantedScore,
                requestIp,
                fingerprint,
                LocalDateTime.now(),
                LocalDateTime.now().plus(ttl)
        ));
        session.removeAttribute(STEP_UP_PENDING);
        session.removeAttribute(STEP_UP_CANDIDATE);
    }

    public boolean hasValidStepUpGrant(HttpSession session,
                                       String loginId,
                                       int requiredScore,
                                       String requestIp,
                                       String fingerprint) {
        StepUpGrant grant = getStepUpGrant(session);
        if (grant == null) {
            return false;
        }
        if (grant.expiresAt().isBefore(LocalDateTime.now())) {
            clearStepUp(session);
            return false;
        }
        return grant.loginId().equals(loginId)
                && grant.grantedScore() >= requiredScore
                && matchesRequestIp(grant.requestIp(), requestIp)
                && matchesFingerprint(grant.fingerprint(), fingerprint);
    }

    public StepUpGrant getStepUpGrant(HttpSession session) {
        Object value = session.getAttribute(STEP_UP_GRANT);
        return value instanceof StepUpGrant grant ? grant : null;
    }

    public void clearStepUp(HttpSession session) {
        session.removeAttribute(STEP_UP_PENDING);
        session.removeAttribute(STEP_UP_CANDIDATE);
        session.removeAttribute(STEP_UP_GRANT);
    }

    public void storeGeneratedBackupCodes(HttpSession session, java.util.List<String> codes) {
        session.setAttribute(GENERATED_BACKUP_CODES, java.util.List.copyOf(codes));
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> getGeneratedBackupCodes(HttpSession session) {
        Object value = session.getAttribute(GENERATED_BACKUP_CODES);
        return value instanceof java.util.List<?> list ? (java.util.List<String>) list : java.util.List.of();
    }

    public void clearGeneratedBackupCodes(HttpSession session) {
        session.removeAttribute(GENERATED_BACKUP_CODES);
    }

    public Duration resolveStepUpTtl() {
        return Duration.ofMinutes(mfaStepUpProperties.getTtlMinutes());
    }

    private boolean safeEquals(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean matchesRequestIp(String stored, String current) {
        return !mfaStepUpProperties.isValidateRequestIp() || safeEquals(stored, current);
    }

    private boolean matchesFingerprint(String stored, String current) {
        return !mfaStepUpProperties.isValidateFingerprint() || safeEquals(stored, current);
    }

    public record StepUpCandidate(String loginId,
                                  int currentScore,
                                  int requiredScore,
                                  String returnUri,
                                  String requestIp,
                                  String fingerprint,
                                  LocalDateTime createdAt) {
    }

    public record StepUpGrant(String loginId,
                              int grantedScore,
                              String requestIp,
                              String fingerprint,
                              LocalDateTime grantedAt,
                              LocalDateTime expiresAt) {
    }
}
