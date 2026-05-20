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
public class TrustOverrideAuditLog {

    public enum ActionType {
        MANUAL_OVERRIDE,
        TEST_BASELINE_RESET
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String operatorLoginId;

    @Column(nullable = false)
    private String targetLoginId;

    @Column(nullable = false)
    private int previousTrustScore;

    @Column(nullable = false)
    private int appliedTrustScore;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(nullable = false)
    private boolean clearContext;

    @Column(length = 500)
    private String reason;

    private String previousFingerprint;

    private LocalDateTime previousLastAccessTime;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
