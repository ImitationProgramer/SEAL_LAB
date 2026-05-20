package com.seal.seal_lab.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "seal.security.zero-trust")
@Getter
@Setter
public class ZeroTrustPolicyProperties {

    private int absoluteMinimumScore = 10;
}
