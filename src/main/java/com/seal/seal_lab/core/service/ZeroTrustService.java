package com.seal.seal_lab.core.service;

import com.seal.seal_lab.api.dto.GeoResponse;
import com.seal.seal_lab.api.dto.TrustDebugView;
import com.seal.seal_lab.core.entity.TrustOverrideAuditLog;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.logging.AdminOperationLogFormatter;
import com.seal.seal_lab.infra.repository.TrustOverrideAuditLogRepository;
import com.seal.seal_lab.infra.repository.UserRepository;
import com.seal.seal_lab.infra.security.DeviceFingerprintResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZeroTrustService {

    private static final double IMPOSSIBLE_TRAVEL_SPEED_KMH = 500.0;
    private static final double FALSE_POSITIVE_DISTANCE_KM = 5.0;
    private static final long FALSE_POSITIVE_TIME_WINDOW_SECONDS = 10L;
    private static final int CROSS_BROWSER_SAME_PLATFORM_PENALTY = 10;
    private static final int UNKNOWN_DEVICE_PENALTY = 25;
    public static final String TEST_BASELINE_FINGERPRINT = "fp:chrome|windows|desktop";

    private final UserRepository userRepository;
    private final TrustOverrideAuditLogRepository trustOverrideAuditLogRepository;
    private final GeoLocationService geoLocationService;

    /**
     * ZTA 핵심 엔진: 실시간 신뢰 점수 산출 및 자가 회복 로직
     */
    @Transactional // 점수 및 시간 업데이트를 위해 트랜잭션 필수
    public int calculateTrustScore(String loginId, String currentIp, String userAgent) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String currentFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(userAgent);
        String registeredFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(user.getLastUserAgent());
        boolean isKnownDevice = user.getLastUserAgent() == null || registeredFingerprint.equals(currentFingerprint);
        RecoveryPreview recoveryPreview = buildRecoveryPreview(user);

        // [STEP 1] 자가 회복(Self-Healing) 로직
        // 마지막 접속 이후 흐른 시간을 계산하여 점수를 회복시킵니다.
        recoverTrustScore(user, recoveryPreview);

        // [STEP 2] 현재 요청에 대한 신뢰도 평가 시작
        // 기본 점수는 회복된 유저의 현재 평판 점수(trustScore)를 기준으로 합니다.
        int currentRequestScore = recoveryPreview.recoveredScore();

        // [PIP 1] Geovelocity 분석 (Impossible Travel)
        GeoResponse currentGeo = geoLocationService.getGeoLocation(currentIp);
        if (currentGeo != null
                && user.getLastLatitude() != null
                && user.getLastLongitude() != null
                && user.getLastAccessTime() != null) {
            double distance = calculateDistance(
                    user.getLastLatitude(), user.getLastLongitude(),
                    currentGeo.getLat(), currentGeo.getLon()
            );

            long seconds = ChronoUnit.SECONDS.between(user.getLastAccessTime(), LocalDateTime.now());

            if (shouldSkipImpossibleTravel(distance, seconds)) {
                log.info("[ZTA-PIP] Impossible Travel 예외 처리 적용 | Distance: {}km | Seconds: {}s",
                        Math.round(distance * 100.0) / 100.0, seconds);
            } else if (seconds > 0) {
                double speed = (distance / (seconds / 3600.0)); // km/h 계산
                if (speed > IMPOSSIBLE_TRAVEL_SPEED_KMH) {
                    log.warn("[ZTA-PIP] Impossible Travel 감지! 시속: {}km/h (-60점)", (int)speed);
                    currentRequestScore -= 60;
                }
            }
        }

        // [PIP 2] 기기 식별 분석
        if (!isKnownDevice) {
            int devicePenalty = resolveDevicePenalty(registeredFingerprint, currentFingerprint);
            log.warn("[ZTA-PIP] 미등록 기기/브라우저 접속 감지 (-{}점) | Registered: {} | Current: {}",
                    devicePenalty, registeredFingerprint, currentFingerprint);
            currentRequestScore -= devicePenalty;
        }

        // [STEP 3] 최종 점수 확정 및 DB 동기화
        int finalScore = Math.max(0, currentRequestScore);

        // 중요: 이번 요청에서 깎인 점수를 유저의 평판(DB)에 즉시 반영합니다.
        // 그래야 다음 클릭 때도 이 낮은 점수에서부터 회복을 시작하게 됩니다.
        user.updateTrustScore(finalScore);

        // 마지막 접속 위치 및 시간 업데이트
        if (currentGeo != null) {
            user.setLastLatitude(currentGeo.getLat());
            user.setLastLongitude(currentGeo.getLon());
        }
        if (isKnownDevice) {
            user.setLastUserAgent(currentFingerprint);
        }
        user.setLastAccessTime(LocalDateTime.now());

        userRepository.save(user);

        log.info("[ZTA-PDP] 최종 신뢰 점수: {}점 (User: {})", finalScore, loginId);
        return finalScore;
    }

    @Transactional(readOnly = true)
    public TrustDebugView inspectCurrentTrustStatus(String loginId, String currentIp, String userAgent) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildTrustDebugView(user, currentIp, userAgent, true);
    }

    @Transactional(readOnly = true)
    public TrustDebugView inspectStoredTrustStatus(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return buildTrustDebugView(user, null, null, false);
    }

    @Transactional
    public void registerTrustedLoginContext(String loginId, String currentIp, String userAgent) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        GeoResponse currentGeo = geoLocationService.getGeoLocation(currentIp);
        if (currentGeo != null) {
            user.setLastLatitude(currentGeo.getLat());
            user.setLastLongitude(currentGeo.getLon());
        }

        String deviceFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(userAgent);
        user.setLastUserAgent(deviceFingerprint);
        user.setLastAccessTime(LocalDateTime.now());

        userRepository.save(user);

        log.info("[ZTA-PIP] 로그인 성공으로 신뢰 기기 컨텍스트 갱신 | User: {} | Fingerprint: {}",
                loginId, deviceFingerprint);
    }

    @Transactional
    public int finalizeSuccessfulLogin(String loginId, String currentIp, String userAgent) {
        int finalScore = calculateTrustScore(loginId, currentIp, userAgent);
        registerTrustedLoginContext(loginId, currentIp, userAgent);
        return finalScore;
    }

    @Transactional
    public void overrideTrustState(String targetLoginId, int trustScore, boolean clearContext, String reason, String operatorLoginId) {
        overrideTrustState(targetLoginId, trustScore, clearContext, reason, null, null, operatorLoginId);
    }

    @Transactional
    public void overrideTrustState(String targetLoginId,
                                   int trustScore,
                                   boolean clearContext,
                                   String reason,
                                   String requestIp,
                                   String sessionFingerprint,
                                   String operatorLoginId) {
        if (targetLoginId == null || targetLoginId.isBlank()) {
            throw new IllegalArgumentException("대상 loginId는 비워둘 수 없습니다.");
        }
        if (trustScore < 0 || trustScore > 100) {
            throw new IllegalArgumentException("신뢰 점수는 0점에서 100점 사이여야 합니다.");
        }
        String normalizedReason = normalizeReason(reason);

        User user = userRepository.findByLoginId(targetLoginId)
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + targetLoginId));

        int previousTrustScore = user.getTrustScore();
        String previousFingerprint = user.getLastUserAgent();
        LocalDateTime previousLastAccessTime = user.getLastAccessTime();
        user.updateTrustScore(trustScore);

        if (clearContext) {
            user.setLastUserAgent(null);
            user.setLastLatitude(null);
            user.setLastLongitude(null);
            user.setLastAccessTime(null);
        }

        userRepository.save(user);
        saveAuditLog(operatorLoginId, targetLoginId, previousTrustScore, trustScore,
                TrustOverrideAuditLog.ActionType.MANUAL_OVERRIDE, clearContext, normalizedReason, previousFingerprint, previousLastAccessTime);

        log.warn("{}", AdminOperationLogFormatter.formatManualOverrideSuccess(
                operatorLoginId,
                targetLoginId,
                previousTrustScore,
                trustScore,
                String.valueOf(clearContext),
                requestIp,
                sessionFingerprint,
                normalizedReason
        ));
    }

    @Transactional
    public void resetTrustStateToTestBaseline(String targetLoginId, String reason, String operatorLoginId) {
        resetTrustStateToTestBaseline(targetLoginId, reason, null, null, operatorLoginId);
    }

    @Transactional
    public void resetTrustStateToTestBaseline(String targetLoginId,
                                              String reason,
                                              String requestIp,
                                              String sessionFingerprint,
                                              String operatorLoginId) {
        if (targetLoginId == null || targetLoginId.isBlank()) {
            throw new IllegalArgumentException("대상 loginId는 비워둘 수 없습니다.");
        }
        String normalizedReason = normalizeReason(reason);

        User user = userRepository.findByLoginId(targetLoginId)
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + targetLoginId));

        int previousTrustScore = user.getTrustScore();
        String previousFingerprint = user.getLastUserAgent();
        LocalDateTime previousLastAccessTime = user.getLastAccessTime();

        user.updateTrustScore(100);
        user.setLastUserAgent(TEST_BASELINE_FINGERPRINT);
        user.setLastLatitude(null);
        user.setLastLongitude(null);
        user.setLastAccessTime(null);
        userRepository.save(user);

        saveAuditLog(operatorLoginId, targetLoginId, previousTrustScore, 100,
                TrustOverrideAuditLog.ActionType.TEST_BASELINE_RESET, true, normalizedReason, previousFingerprint, previousLastAccessTime);

        log.warn("{}", AdminOperationLogFormatter.formatTestBaselineSuccess(
                operatorLoginId,
                targetLoginId,
                previousTrustScore,
                requestIp,
                sessionFingerprint,
                TEST_BASELINE_FINGERPRINT,
                normalizedReason
        ));
    }

    @Transactional(readOnly = true)
    public java.util.List<TrustOverrideAuditLog> getRecentTrustOverrideLogs() {
        return trustOverrideAuditLogRepository.findTop30ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public java.util.List<TrustOverrideAuditLog> searchTrustOverrideLogs(String operatorLoginId,
                                                                         String targetLoginId,
                                                                         String actionTypeFilter,
                                                                         String contextResetFilter,
                                                                         LocalDate dateFrom,
                                                                         LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }

        return trustOverrideAuditLogRepository.searchAuditLogs(
                normalizeFilter(operatorLoginId),
                normalizeFilter(targetLoginId),
                normalizeActionTypeFilter(actionTypeFilter),
                normalizeContextResetFilter(contextResetFilter),
                dateFrom != null ? dateFrom.atStartOfDay() : null,
                dateTo != null ? dateTo.atTime(23, 59, 59) : null,
                PageRequest.of(0, 50)
        );
    }

    private void saveAuditLog(String operatorLoginId,
                              String targetLoginId,
                              int previousTrustScore,
                              int appliedTrustScore,
                              TrustOverrideAuditLog.ActionType actionType,
                              boolean clearContext,
                              String reason,
                              String previousFingerprint,
                              LocalDateTime previousLastAccessTime) {
        trustOverrideAuditLogRepository.save(TrustOverrideAuditLog.builder()
                .operatorLoginId(operatorLoginId)
                .targetLoginId(targetLoginId)
                .previousTrustScore(previousTrustScore)
                .appliedTrustScore(appliedTrustScore)
                .actionType(actionType)
                .clearContext(clearContext)
                .reason(reason)
                .previousFingerprint(previousFingerprint)
                .previousLastAccessTime(previousLastAccessTime)
                .build());
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeReason(String reason) {
        String normalizedReason = normalizeFilter(reason);
        if (normalizedReason == null) {
            throw new IllegalArgumentException("운영 사유는 비워둘 수 없습니다.");
        }
        if (normalizedReason.length() > 500) {
            throw new IllegalArgumentException("운영 사유는 500자를 넘길 수 없습니다.");
        }
        return normalizedReason;
    }

    private TrustOverrideAuditLog.ActionType normalizeActionTypeFilter(String actionTypeFilter) {
        String normalized = normalizeFilter(actionTypeFilter);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }

        try {
            return TrustOverrideAuditLog.ActionType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 actionType 필터입니다.");
        }
    }

    private Boolean normalizeContextResetFilter(String contextResetFilter) {
        String normalized = normalizeFilter(contextResetFilter);
        if (normalized == null || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        if ("CLEARED".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("PRESERVED".equalsIgnoreCase(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("지원하지 않는 context reset 필터입니다.");
    }

    /**
     * 시간 기반 신뢰도 회복 로직 (10분당 1점 회복 정책)
     */
    private void recoverTrustScore(User user, RecoveryPreview recoveryPreview) {
        if (recoveryPreview.recoveredPoints() <= 0) {
            return;
        }

        log.info("[ZTA-Recovery] {}분 경과로 신뢰도 회복: {} -> {}",
                recoveryPreview.minutesPassed(), user.getTrustScore(), recoveryPreview.recoveredScore());

        user.updateTrustScore(recoveryPreview.recoveredScore());
    }

    private boolean shouldSkipImpossibleTravel(double distance, long seconds) {
        return distance < FALSE_POSITIVE_DISTANCE_KM || seconds <= FALSE_POSITIVE_TIME_WINDOW_SECONDS;
    }

    private int resolveDevicePenalty(String registeredFingerprint, String currentFingerprint) {
        Fingerprint registered = Fingerprint.from(registeredFingerprint);
        Fingerprint current = Fingerprint.from(currentFingerprint);

        if (registered.isSamePlatform(current) && !registered.browser().equals(current.browser())) {
            return CROSS_BROWSER_SAME_PLATFORM_PENALTY;
        }

        return UNKNOWN_DEVICE_PENALTY;
    }

    private TrustDebugView buildTrustDebugView(User user, String currentIp, String userAgent, boolean liveEvaluation) {
        RecoveryPreview recoveryPreview = buildRecoveryPreview(user);

        String registeredFingerprint = DeviceFingerprintResolver.toDeviceFingerprint(user.getLastUserAgent());
        String currentFingerprint = liveEvaluation ? DeviceFingerprintResolver.toDeviceFingerprint(userAgent) : null;

        GeoResponse currentGeo = liveEvaluation ? geoLocationService.getGeoLocation(currentIp) : null;

        int devicePenalty = 0;
        String deviceReason = "현재 등록된 기기와 동일합니다.";
        if (liveEvaluation && user.getLastUserAgent() != null && !registeredFingerprint.equals(currentFingerprint)) {
            devicePenalty = resolveDevicePenalty(registeredFingerprint, currentFingerprint);
            deviceReason = (devicePenalty == CROSS_BROWSER_SAME_PLATFORM_PENALTY)
                    ? "같은 플랫폼에서 브라우저만 변경되어 경감 감점이 적용됩니다."
                    : "등록된 플랫폼과 다른 기기 또는 운영체제로 판단됩니다.";
        } else if (liveEvaluation && user.getLastUserAgent() == null) {
            deviceReason = "등록된 기기 정보가 없어 이번 세션을 기준선으로 사용합니다.";
        }

        int impossibleTravelPenalty = 0;
        String impossibleTravelReason = "판단 가능한 위치 정보가 아직 충분하지 않습니다.";
        if (liveEvaluation && currentGeo != null
                && user.getLastLatitude() != null
                && user.getLastLongitude() != null
                && user.getLastAccessTime() != null) {
            double distance = calculateDistance(
                    user.getLastLatitude(), user.getLastLongitude(),
                    currentGeo.getLat(), currentGeo.getLon()
            );
            long seconds = ChronoUnit.SECONDS.between(user.getLastAccessTime(), LocalDateTime.now());

            if (shouldSkipImpossibleTravel(distance, seconds)) {
                impossibleTravelReason = "근거리 이동 또는 짧은 요청 간격으로 판단되어 예외 처리되었습니다.";
            } else if (seconds > 0) {
                double speed = distance / (seconds / 3600.0);
                if (speed > IMPOSSIBLE_TRAVEL_SPEED_KMH) {
                    impossibleTravelPenalty = 60;
                    impossibleTravelReason = "Impossible Travel 패턴이 감지되어 60점 감점됩니다.";
                } else {
                    impossibleTravelReason = "현재 위치 이동 속도는 정상 범위입니다.";
                }
            }
        } else if (liveEvaluation && currentGeo != null) {
            impossibleTravelReason = "이전 위치 또는 접속 시간이 없어 비교 기준이 아직 없습니다.";
        }

        int evaluatedTrustScore = Math.max(0, recoveryPreview.recoveredScore() - devicePenalty - impossibleTravelPenalty);

        return TrustDebugView.builder()
                .loginId(user.getLoginId())
                .role(user.getRole().name())
                .storedTrustScore(user.getTrustScore())
                .evaluatedTrustScore(liveEvaluation ? evaluatedTrustScore : null)
                .recoveredTrustScore(recoveryPreview.recoveredScore())
                .recoveredPoints(recoveryPreview.recoveredPoints())
                .minutesSinceLastAccess(recoveryPreview.minutesPassed())
                .devicePenalty(devicePenalty)
                .deviceReason(deviceReason)
                .impossibleTravelPenalty(impossibleTravelPenalty)
                .impossibleTravelReason(impossibleTravelReason)
                .currentIp(currentIp)
                .currentFingerprint(currentFingerprint)
                .registeredFingerprint(registeredFingerprint)
                .currentLocation(formatGeo(currentGeo))
                .lastLocation(formatGeo(user.getLastLatitude(), user.getLastLongitude()))
                .lastAccessTime(user.getLastAccessTime() != null ? user.getLastAccessTime().toString() : "기록 없음")
                .riskLevel(resolveRiskLevel(liveEvaluation ? evaluatedTrustScore : recoveryPreview.recoveredScore()))
                .adminActionAllowed(liveEvaluation && evaluatedTrustScore >= 90)
                .liveEvaluation(liveEvaluation)
                .build();
    }

    private RecoveryPreview buildRecoveryPreview(User user) {
        if (user.getLastAccessTime() == null || user.getTrustScore() >= 100) {
            return new RecoveryPreview(user.getTrustScore(), 0, 0);
        }

        long minutesPassed = ChronoUnit.MINUTES.between(user.getLastAccessTime(), LocalDateTime.now());
        if (minutesPassed < 10) {
            return new RecoveryPreview(user.getTrustScore(), 0, minutesPassed);
        }

        int recoveryPoints = (int) (minutesPassed / 10);
        int recoveredScore = Math.min(100, user.getTrustScore() + recoveryPoints);
        return new RecoveryPreview(recoveredScore, recoveredScore - user.getTrustScore(), minutesPassed);
    }

    private String resolveRiskLevel(int score) {
        if (score >= 90) {
            return "Operational";
        }
        if (score >= 60) {
            return "Caution";
        }
        return "Restricted";
    }

    private String formatGeo(GeoResponse geoResponse) {
        if (geoResponse == null) {
            return "위치 정보 없음";
        }
        return formatGeo(geoResponse.getLat(), geoResponse.getLon());
    }

    private String formatGeo(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return "위치 정보 없음";
        }
        return String.format(Locale.ROOT, "%.4f, %.4f", latitude, longitude);
    }

    private record Fingerprint(String browser, String operatingSystem, String deviceType) {

        private static Fingerprint from(String fingerprint) {
            String normalized = fingerprint == null ? "" : fingerprint;
            if (normalized.startsWith("fp:")) {
                normalized = normalized.substring(3);
            }

            String[] tokens = normalized.split("\\|", -1);
            if (tokens.length != 3) {
                return new Fingerprint("unknown", "unknown", "unknown");
            }

            return new Fingerprint(tokens[0], tokens[1], tokens[2]);
        }

        private boolean isSamePlatform(Fingerprint other) {
            return operatingSystem.equals(other.operatingSystem) && deviceType.equals(other.deviceType);
        }
    }

    private record RecoveryPreview(int recoveredScore, int recoveredPoints, long minutesPassed) {
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
        dist = Math.acos(dist);
        dist = Math.toDegrees(dist);
        return dist * 60 * 1.1515 * 1.609344;
    }
}
