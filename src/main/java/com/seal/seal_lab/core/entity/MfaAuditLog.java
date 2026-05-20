package com.seal.seal_lab.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MfaAuditLog {

    public enum FlowType {
        LOGIN_SETUP,
        LOGIN_VERIFY,
        STEP_UP,
        BACKUP_CODE_LOGIN,
        BACKUP_CODE_STEP_UP,
        BACKUP_CODE_REISSUE,
        ADMIN_MFA_RESET,
        ADMIN_PASSWORD_CHANGE,
        MEMBER_PASSWORD_CHANGE,
        ADMIN_PASSWORD_RESET_ISSUE,
        ADMIN_PASSWORD_RESET_COMPLETE,
        MEMBER_PASSWORD_RESET_ISSUE,
        MEMBER_PASSWORD_RESET_COMPLETE,
        PASSWORD_SESSION_REVOKE
    }

    public enum ResultType {
        STARTED,
        SUCCESS,
        FAILURE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String loginId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlowType flowType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultType resultType;

    private Integer currentTrustScore;

    private Integer requiredTrustScore;

    private Integer grantedTrustScore;

    private String requestIp;

    private String sessionFingerprint;

    @Column(length = 500)
    private String returnUri;

    @Column(length = 500)
    private String detailMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
