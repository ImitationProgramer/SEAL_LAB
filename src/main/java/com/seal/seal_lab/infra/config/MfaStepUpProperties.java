package com.seal.seal_lab.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seal.security.mfa.step-up")
@Getter
@Setter
public class MfaStepUpProperties {

    private long ttlMinutes = 10;
    private boolean validateRequestIp = true;
    private boolean validateFingerprint = true;
}
