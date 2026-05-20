package com.seal.seal_lab.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrustDebugView {

    private final String loginId;
    private final String role;
    private final int storedTrustScore;
    private final Integer evaluatedTrustScore;
    private final int recoveredTrustScore;
    private final int recoveredPoints;
    private final long minutesSinceLastAccess;
    private final int devicePenalty;
    private final String deviceReason;
    private final int impossibleTravelPenalty;
    private final String impossibleTravelReason;
    private final String currentIp;
    private final String currentFingerprint;
    private final String registeredFingerprint;
    private final String currentLocation;
    private final String lastLocation;
    private final String lastAccessTime;
    private final String riskLevel;
    private final boolean adminActionAllowed;
    private final boolean liveEvaluation;
}
