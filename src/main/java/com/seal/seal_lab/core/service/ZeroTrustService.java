package com.seal.seal_lab.core.service;

import com.seal.seal_lab.api.dto.GeoResponse;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZeroTrustService {

    private final UserRepository userRepository;
    private final GeoLocationService geoLocationService;

    /**
     * ZTA 핵심 엔진: 실시간 신뢰 점수 산출 및 자가 회복 로직
     */
    @Transactional // 점수 및 시간 업데이트를 위해 트랜잭션 필수
    public int calculateTrustScore(String loginId, String currentIp, String userAgent) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // [STEP 1] 자가 회복(Self-Healing) 로직
        // 마지막 접속 이후 흐른 시간을 계산하여 점수를 회복시킵니다.
        recoverTrustScore(user);

        // [STEP 2] 현재 요청에 대한 신뢰도 평가 시작
        // 기본 점수는 회복된 유저의 현재 평판 점수(trustScore)를 기준으로 합니다.
        int currentRequestScore = user.getTrustScore();

        // [PIP 1] Geovelocity 분석 (Impossible Travel)
        GeoResponse currentGeo = geoLocationService.getGeoLocation(currentIp);
        if (currentGeo != null && user.getLastLatitude() != null && user.getLastAccessTime() != null) {
            double distance = calculateDistance(
                    user.getLastLatitude(), user.getLastLongitude(),
                    currentGeo.getLat(), currentGeo.getLon()
            );

            long seconds = ChronoUnit.SECONDS.between(user.getLastAccessTime(), LocalDateTime.now());

            if (seconds > 0) {
                double speed = (distance / (seconds / 3600.0)); // km/h 계산
                if (speed > 500) {
                    log.warn("[ZTA-PIP] Impossible Travel 감지! 시속: {}km/h (-60점)", (int)speed);
                    currentRequestScore -= 60;
                }
            }
        }

        // [PIP 2] 기기 식별 분석
        if (user.getLastUserAgent() != null && !userAgent.equals(user.getLastUserAgent())) {
            log.warn("[ZTA-PIP] 미등록 기기/브라우저 접속 감지 (-25점)");
            currentRequestScore -= 25;
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
        user.setLastUserAgent(userAgent);
        user.setLastAccessTime(LocalDateTime.now());

        userRepository.save(user);

        log.info("[ZTA-PDP] 최종 신뢰 점수: {}점 (User: {})", finalScore, loginId);
        return finalScore;
    }

    /**
     * 시간 기반 신뢰도 회복 로직 (10분당 1점 회복 정책)
     */
    private void recoverTrustScore(User user) {
        if (user.getLastAccessTime() == null || user.getTrustScore() >= 100) return;

        long minutesPassed = ChronoUnit.MINUTES.between(user.getLastAccessTime(), LocalDateTime.now());

        if (minutesPassed >= 10) {
            int recoveryPoints = (int) (minutesPassed / 10);
            int newScore = Math.min(100, user.getTrustScore() + recoveryPoints);

            log.info("[ZTA-Recovery] {}분 경과로 신뢰도 회복: {} -> {}",
                    minutesPassed, user.getTrustScore(), newScore);

            user.updateTrustScore(newScore);
        }
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