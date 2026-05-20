package com.seal.seal_lab.api.service;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyMemberMigrationServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private LegacyMemberMigrationService legacyMemberMigrationService;

    private User jspark;

    @BeforeEach
    void setUp() {
        jspark = User.builder()
                .loginId("jspark")
                .name("교수")
                .email("jspark@example.com")
                .role(User.Role.ADMIN)
                .labRank(User.LabRank.GENERAL_PUBLIC)
                .trustScore(100)
                .build();
    }

    @Test
    void promotesJsparkToProfessorWhenAdmin() {
        when(userRepository.findByLoginId("jspark")).thenReturn(Optional.of(jspark));
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 'member'")).thenReturn(List.of());

        legacyMemberMigrationService.migrateLegacyMembersIfPresent();

        assertThat(jspark.getLabRank()).isEqualTo(User.LabRank.PROFESSOR);
        verify(userRepository).save(jspark);
    }

    @Test
    void migratesLegacyMemberProfileByExactEmailMatch() {
        User intern = User.builder()
                .loginId("intern1")
                .name("홍길동")
                .email("intern@example.com")
                .role(User.Role.MEMBER)
                .labRank(User.LabRank.GENERAL_PUBLIC)
                .trustScore(100)
                .build();

        when(userRepository.findByLoginId("jspark")).thenReturn(Optional.empty());
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 'member'"))
                .thenReturn(List.of(Map.of("Tables_in_seal_lab_db (member)", "member")));
        when(jdbcTemplate.queryForList("SELECT id, name, degree, role, department, email, keywords, image_path FROM member"))
                .thenReturn(List.of(Map.of(
                        "id", 1L,
                        "name", "홍길동",
                        "degree", "학부연구생",
                        "role", "Undergraduate Researcher",
                        "department", "Computer Science",
                        "email", "intern@example.com",
                        "keywords", "Zero Trust, Malware",
                        "image_path", "/uploads/intern.png"
                )));
        when(userRepository.findByEmail("intern@example.com")).thenReturn(Optional.of(intern));

        legacyMemberMigrationService.migrateLegacyMembersIfPresent();

        assertThat(intern.getLabRank()).isEqualTo(User.LabRank.UNDERGRAD_RESEARCHER);
        assertThat(intern.getDepartment()).isEqualTo("Computer Science");
        assertThat(intern.getKeywords()).isEqualTo("Zero Trust, Malware");
        assertThat(intern.getImagePath()).isEqualTo("/uploads/intern.png");
        verify(userRepository).save(intern);
    }

    @Test
    void skipsLegacyMigrationWhenTableDoesNotExist() {
        when(userRepository.findByLoginId("jspark")).thenReturn(Optional.empty());
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 'member'")).thenReturn(List.of());

        legacyMemberMigrationService.migrateLegacyMembersIfPresent();

        verify(jdbcTemplate, never())
                .queryForList("SELECT id, name, degree, role, department, email, keywords, image_path FROM member");
    }

    @Test
    void mapLegacyRankSupportsProfessorAndIntern() {
        assertThat(legacyMemberMigrationService.mapLegacyRank("교수", null)).isEqualTo(User.LabRank.PROFESSOR);
        assertThat(legacyMemberMigrationService.mapLegacyRank("인턴", null)).isEqualTo(User.LabRank.INTERN);
    }
}
