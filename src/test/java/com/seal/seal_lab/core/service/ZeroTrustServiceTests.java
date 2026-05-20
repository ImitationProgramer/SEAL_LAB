package com.seal.seal_lab.core.service;

import com.seal.seal_lab.api.dto.GeoResponse;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.TrustOverrideAuditLogRepository;
import com.seal.seal_lab.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZeroTrustServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GeoLocationService geoLocationService;

    @Mock
    private TrustOverrideAuditLogRepository trustOverrideAuditLogRepository;

    @InjectMocks
    private ZeroTrustService zeroTrustService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .loginId("tester")
                .name("테스터")
                .role(User.Role.ADMIN)
                .trustScore(100)
                .lastLatitude(37.2758)
                .lastLongitude(127.1325)
                .lastUserAgent("same-agent")
                .build();
    }

    @Test
    void impossibleTravelIsSkippedWhenDistanceIsLessThanFiveKilometers() {
        user.setLastAccessTime(LocalDateTime.now().minusSeconds(1));

        GeoResponse currentGeo = new GeoResponse();
        currentGeo.setLat(37.2850);
        currentGeo.setLon(127.1325);

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(geoLocationService.getGeoLocation("1.1.1.1")).thenReturn(currentGeo);

        int score = zeroTrustService.calculateTrustScore("tester", "1.1.1.1", "same-agent");

        assertThat(score).isEqualTo(100);
        assertThat(user.getTrustScore()).isEqualTo(100);
        verify(userRepository).save(user);
    }

    @Test
    void impossibleTravelIsSkippedWhenRequestIntervalIsTenSecondsOrLess() {
        user.setLastAccessTime(LocalDateTime.now().minusSeconds(5));

        GeoResponse currentGeo = new GeoResponse();
        currentGeo.setLat(38.2758);
        currentGeo.setLon(127.1325);

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(geoLocationService.getGeoLocation("1.1.1.1")).thenReturn(currentGeo);

        int score = zeroTrustService.calculateTrustScore("tester", "1.1.1.1", "same-agent");

        assertThat(score).isEqualTo(100);
        assertThat(user.getTrustScore()).isEqualTo(100);
        verify(userRepository).save(user);
    }

    @Test
    void impossibleTravelStillDeductsScoreWhenDistanceAndTimeThresholdsAreExceeded() {
        user.setLastAccessTime(LocalDateTime.now().minusSeconds(30));

        GeoResponse currentGeo = new GeoResponse();
        currentGeo.setLat(38.2758);
        currentGeo.setLon(127.1325);

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(geoLocationService.getGeoLocation("1.1.1.1")).thenReturn(currentGeo);

        int score = zeroTrustService.calculateTrustScore("tester", "1.1.1.1", "same-agent");

        assertThat(score).isEqualTo(40);
        assertThat(user.getTrustScore()).isEqualTo(40);
        verify(userRepository).save(user);
    }

    @Test
    void sameBrowserFamilyDoesNotDeductScoreWhenOnlyVersionChanges() {
        user.setLastAccessTime(LocalDateTime.now().minusMinutes(1));
        user.setLastUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36");

        GeoResponse currentGeo = new GeoResponse();
        currentGeo.setLat(37.2758);
        currentGeo.setLon(127.1325);

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(geoLocationService.getGeoLocation("1.1.1.1")).thenReturn(currentGeo);

        int score = zeroTrustService.calculateTrustScore(
                "tester",
                "1.1.1.1",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
        );

        assertThat(score).isEqualTo(100);
        assertThat(user.getTrustScore()).isEqualTo(100);
        assertThat(user.getLastUserAgent()).isEqualTo("fp:chrome|macos|desktop");
        verify(userRepository).save(user);
    }

    @Test
    void unknownDeviceDeductsScoreButDoesNotOverwriteRegisteredFingerprint() {
        user.setLastAccessTime(LocalDateTime.now().minusMinutes(1));
        user.setLastUserAgent("fp:chrome|macos|desktop");

        GeoResponse currentGeo = new GeoResponse();
        currentGeo.setLat(37.2758);
        currentGeo.setLon(127.1325);

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(geoLocationService.getGeoLocation("1.1.1.1")).thenReturn(currentGeo);

        int score = zeroTrustService.calculateTrustScore(
                "tester",
                "1.1.1.1",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:138.0) Gecko/20100101 Firefox/138.0"
        );

        assertThat(score).isEqualTo(75);
        assertThat(user.getTrustScore()).isEqualTo(75);
        assertThat(user.getLastUserAgent()).isEqualTo("fp:chrome|macos|desktop");
        verify(userRepository).save(user);
    }

    @Test
    void crossBrowserOnSameDesktopPlatformUsesReducedPenalty() {
        user.setLastAccessTime(LocalDateTime.now().minusMinutes(1));
        user.setLastUserAgent("fp:chrome|windows|desktop");

        GeoResponse currentGeo = new GeoResponse();
        currentGeo.setLat(37.2758);
        currentGeo.setLon(127.1325);

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(geoLocationService.getGeoLocation("1.1.1.1")).thenReturn(currentGeo);

        int score = zeroTrustService.calculateTrustScore(
                "tester",
                "1.1.1.1",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:138.0) Gecko/20100101 Firefox/138.0"
        );

        assertThat(score).isEqualTo(90);
        assertThat(user.getTrustScore()).isEqualTo(90);
        assertThat(user.getLastUserAgent()).isEqualTo("fp:chrome|windows|desktop");
        verify(userRepository).save(user);
    }

    @Test
    void trustedLoginContextRegistersNewBrowserFingerprintAfterSuccessfulLogin() {
        GeoResponse currentGeo = new GeoResponse();
        currentGeo.setLat(37.2758);
        currentGeo.setLon(127.1325);

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));
        when(geoLocationService.getGeoLocation("1.1.1.1")).thenReturn(currentGeo);

        zeroTrustService.registerTrustedLoginContext(
                "tester",
                "1.1.1.1",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
        );

        assertThat(user.getLastUserAgent()).isEqualTo("fp:chrome|windows|desktop");
        verify(userRepository).save(user);
    }

    @Test
    void resetTrustStateToTestBaselineAppliesPresetScoreAndFingerprint() {
        user.setTrustScore(42);
        user.setLastUserAgent("fp:firefox|windows|desktop");
        user.setLastLatitude(37.2758);
        user.setLastLongitude(127.1325);
        user.setLastAccessTime(LocalDateTime.now().minusMinutes(3));

        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));

        zeroTrustService.resetTrustStateToTestBaseline("tester", "브라우저 테스트 초기화", "admin");

        assertThat(user.getTrustScore()).isEqualTo(100);
        assertThat(user.getLastUserAgent()).isEqualTo(ZeroTrustService.TEST_BASELINE_FINGERPRINT);
        assertThat(user.getLastLatitude()).isNull();
        assertThat(user.getLastLongitude()).isNull();
        assertThat(user.getLastAccessTime()).isNull();
        verify(userRepository).save(user);
        verify(trustOverrideAuditLogRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getActionType() == com.seal.seal_lab.core.entity.TrustOverrideAuditLog.ActionType.TEST_BASELINE_RESET
                        && log.getAppliedTrustScore() == 100
                        && "브라우저 테스트 초기화".equals(log.getReason())
                        && log.isClearContext()
        ));
    }

    @Test
    void overrideTrustStateRejectsOutOfRangeScore() {
        assertThatThrownBy(() -> zeroTrustService.overrideTrustState("tester", 101, false, "잘못된 점수", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("신뢰 점수는 0점에서 100점 사이여야 합니다.");

        verify(userRepository, never()).save(user);
        verify(trustOverrideAuditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void overrideTrustStateFailsWhenTargetUserDoesNotExist() {
        when(userRepository.findByLoginId("missing-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> zeroTrustService.overrideTrustState("missing-user", 80, false, "없는 계정 점검", "admin"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("대상 사용자를 찾을 수 없습니다: missing-user");

        verify(userRepository, never()).save(user);
        verify(trustOverrideAuditLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void overrideTrustStateRejectsBlankReason() {
        assertThatThrownBy(() -> zeroTrustService.overrideTrustState("tester", 90, false, "   ", "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("운영 사유는 비워둘 수 없습니다.");
    }

    @Test
    void overrideTrustStateStoresReasonInAuditLog() {
        when(userRepository.findByLoginId("tester")).thenReturn(Optional.of(user));

        zeroTrustService.overrideTrustState("tester", 88, true, "오탐 수동 보정", "admin");

        verify(trustOverrideAuditLogRepository).save(org.mockito.ArgumentMatchers.argThat(log ->
                log.getActionType() == com.seal.seal_lab.core.entity.TrustOverrideAuditLog.ActionType.MANUAL_OVERRIDE
                        && "오탐 수동 보정".equals(log.getReason())
                        && log.isClearContext()
        ));
    }

    @Test
    void searchTrustOverrideLogsRejectsReversedDateRange() {
        assertThatThrownBy(() -> zeroTrustService.searchTrustOverrideLogs("admin", "jspark", null, null,
                LocalDate.of(2026, 5, 19), LocalDate.of(2026, 5, 18)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일은 종료일보다 늦을 수 없습니다.");
    }

    @Test
    void searchTrustOverrideLogsNormalizesFiltersAndUsesInclusiveDateRange() {
        zeroTrustService.searchTrustOverrideLogs(" admin ", " jspark ", "manual_override", "preserved",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18));

        verify(trustOverrideAuditLogRepository).searchAuditLogs(
                org.mockito.ArgumentMatchers.eq("admin"),
                org.mockito.ArgumentMatchers.eq("jspark"),
                org.mockito.ArgumentMatchers.eq(com.seal.seal_lab.core.entity.TrustOverrideAuditLog.ActionType.MANUAL_OVERRIDE),
                org.mockito.ArgumentMatchers.eq(false),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 5, 1).atStartOfDay()),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 5, 18).atTime(23, 59, 59)),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        );
    }

    @Test
    void searchTrustOverrideLogsRejectsUnknownActionTypeFilter() {
        assertThatThrownBy(() -> zeroTrustService.searchTrustOverrideLogs("admin", "jspark", "invalid_type", null,
                null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 actionType 필터입니다.");
    }

    @Test
    void searchTrustOverrideLogsRejectsUnknownContextResetFilter() {
        assertThatThrownBy(() -> zeroTrustService.searchTrustOverrideLogs("admin", "jspark", null, "unknown_state",
                null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 context reset 필터입니다.");
    }
}
