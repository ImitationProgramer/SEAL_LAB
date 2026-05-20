package com.seal.seal_lab.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "admin.initial")
public class AdminBootstrapProperties {

    private boolean enabled = true;
    private String loginId = "jspark0427";
    private String password;
}
