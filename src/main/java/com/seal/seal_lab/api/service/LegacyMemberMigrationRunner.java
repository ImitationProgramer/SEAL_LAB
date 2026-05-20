package com.seal.seal_lab.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(JdbcTemplate.class)
@ConditionalOnProperty(name = "seal.migration.legacy-member.enabled", havingValue = "true", matchIfMissing = true)
public class LegacyMemberMigrationRunner implements ApplicationRunner {

    private final LegacyMemberMigrationService legacyMemberMigrationService;

    @Override
    public void run(ApplicationArguments args) {
        legacyMemberMigrationService.migrateLegacyMembersIfPresent();
    }
}
