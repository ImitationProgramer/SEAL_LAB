package com.seal.seal_lab.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter // 점수 및 위치 업데이트를 위해 Setter 추가 (필요에 따라 선별적 허용 가능)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    private String password;

    @Column(nullable = false)
    private String name;

    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String provider; // OAuth2용 혹은 기기 식별 정보(User-Agent) 저장용

    /**
     * [Zero Trust 핵심 필드 1] 실시간 신뢰 점수 (0 ~ 100)
     */
    @Column(nullable = false)
    private int trustScore;

    /**
     * [Zero Trust 핵심 필드 2] Geovelocity (Impossible Travel) 판단용
     */
    private Double lastLatitude;   // 마지막 접속 위도
    private Double lastLongitude;  // 마지막 접속 경도
    private LocalDateTime lastAccessTime; // 마지막 접속 시간

    /**
     * [Zero Trust 핵심 필드 3] 기기 식별용
     */
    private String lastUserAgent; // 최근 접속 기기 정보

    @PrePersist
    public void prePersist() {
        if (this.trustScore == 0) {
            this.trustScore = 100; // 제로 트러스트의 시작은 통상 '신뢰 상태(100)'에서 감점하는 방식을 권장합니다.
        }
    }

    public enum Role {
        ADMIN, MEMBER
    }

    // --- 비즈니스 로직 (도메인 주도 설계 방식) ---

    /**
     * 신뢰 점수 업데이트 (0~100 사이 유지)
     */
    public void updateTrustScore(int newScore) {
        this.trustScore = Math.max(0, Math.min(100, newScore));
    }

    /**
     * 접속 컨텍스트 업데이트 (로그인 성공 혹은 페이지 이동 시 갱신)
     */
    public void updateContext(Double lat, Double lng, String userAgent) {
        this.lastLatitude = lat;
        this.lastLongitude = lng;
        this.lastUserAgent = userAgent;
        this.lastAccessTime = LocalDateTime.now();
    }
}