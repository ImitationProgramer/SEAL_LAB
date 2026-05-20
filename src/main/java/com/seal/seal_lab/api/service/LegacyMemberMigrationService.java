package com.seal.seal_lab.api.service;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(JdbcTemplate.class)
@Transactional
public class LegacyMemberMigrationService {

    private static final String LEGACY_MEMBER_TABLE = "member";

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public void migrateLegacyMembersIfPresent() {
        promoteProfessorAccountIfNeeded();

        if (!legacyMemberTableExists()) {
            log.info("[LEGACY-MEMBER-MIGRATION] legacy member table not found. skipping.");
            return;
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, degree, role, department, email, keywords, image_path FROM member"
        );

        int migrated = 0;
        int skipped = 0;

        for (Map<String, Object> row : rows) {
            Optional<User> userOptional = findTargetUser(row);
            if (userOptional.isEmpty()) {
                skipped++;
                log.warn("[LEGACY-MEMBER-MIGRATION] skipped row. no matching user. row={}", sanitizeRow(row));
                continue;
            }

            User user = userOptional.get();
            boolean changed = applyLegacyProfile(user, row);
            if (changed) {
                userRepository.save(user);
                migrated++;
                log.info("[LEGACY-MEMBER-MIGRATION] migrated profile into user. loginId={}", user.getLoginId());
            }
        }

        log.info("[LEGACY-MEMBER-MIGRATION] completed. migrated={}, skipped={}", migrated, skipped);
    }

    void promoteProfessorAccountIfNeeded() {
        userRepository.findByLoginId("jspark0427").ifPresent(user -> {
            if (user.getRole() == User.Role.ADMIN && user.getResolvedLabRank() != User.LabRank.PROFESSOR) {
                user.setLabRank(User.LabRank.PROFESSOR);
                userRepository.save(user);
                log.info("[LEGACY-MEMBER-MIGRATION] promoted jspark0427 to PROFESSOR.");
            }
        });
    }

    boolean legacyMemberTableExists() {
        List<Map<String, Object>> result = jdbcTemplate.queryForList("SHOW TABLES LIKE '" + LEGACY_MEMBER_TABLE + "'");
        return !result.isEmpty();
    }

    private Optional<User> findTargetUser(Map<String, Object> row) {
        String email = asText(row.get("email"));
        if (StringUtils.hasText(email)) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }

        String name = asText(row.get("name"));
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }

        List<User> users = userRepository.findAllByName(name);
        return users.size() == 1 ? Optional.of(users.getFirst()) : Optional.empty();
    }

    private boolean applyLegacyProfile(User user, Map<String, Object> row) {
        boolean changed = false;

        changed |= fillIfBlank(user::getDepartment, user::setDepartment, asText(row.get("department")));
        changed |= fillIfBlank(user::getKeywords, user::setKeywords, asText(row.get("keywords")));
        changed |= fillIfBlank(user::getImagePath, user::setImagePath, asText(row.get("image_path")));

        String legacyName = asText(row.get("name"));
        if (!StringUtils.hasText(user.getName()) && StringUtils.hasText(legacyName)) {
            user.setName(legacyName);
            changed = true;
        }

        User.LabRank mappedRank = mapLegacyRank(asText(row.get("degree")), asText(row.get("role")));
        if (mappedRank != null && user.getResolvedLabRank() == User.LabRank.GENERAL_PUBLIC) {
            user.setLabRank(mappedRank);
            changed = true;
        }

        return changed;
    }

    private boolean fillIfBlank(ValueReader reader, ValueWriter writer, String incoming) {
        if (!StringUtils.hasText(incoming) || StringUtils.hasText(reader.read())) {
            return false;
        }
        writer.write(incoming);
        return true;
    }

    User.LabRank mapLegacyRank(String degree, String role) {
        String combined = ((degree == null ? "" : degree) + " " + (role == null ? "" : role))
                .toLowerCase(Locale.ROOT);

        if (combined.contains("교수") || combined.contains("professor")) {
            return User.LabRank.PROFESSOR;
        }
        if (combined.contains("학부연구생") || combined.contains("undergraduate researcher")) {
            return User.LabRank.UNDERGRAD_RESEARCHER;
        }
        if (combined.contains("석사") || combined.contains("graduate") || combined.contains("master")) {
            return User.LabRank.MASTER_STUDENT;
        }
        if (combined.contains("인턴") || combined.contains("intern")) {
            return User.LabRank.INTERN;
        }

        return null;
    }

    private Map<String, Object> sanitizeRow(Map<String, Object> row) {
        return Map.of(
                "id", row.get("id"),
                "name", row.get("name"),
                "email", row.get("email"),
                "degree", row.get("degree"),
                "role", row.get("role")
        );
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    @FunctionalInterface
    interface ValueReader {
        String read();
    }

    @FunctionalInterface
    interface ValueWriter {
        void write(String value);
    }
}
