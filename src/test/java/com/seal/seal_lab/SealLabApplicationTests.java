package com.seal.seal_lab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.seal.seal_lab.infra.repository.ContactInfoRepository;
import com.seal.seal_lab.infra.repository.GalleryRepository;
import com.seal.seal_lab.infra.repository.LabIntroRepository;
import com.seal.seal_lab.infra.repository.MfaAuditLogRepository;
import com.seal.seal_lab.infra.repository.MfaBackupCodeRepository;
import com.seal.seal_lab.infra.repository.NewsRepository;
import com.seal.seal_lab.infra.repository.PasswordResetTokenRepository;
import com.seal.seal_lab.infra.repository.ProfessorRepository;
import com.seal.seal_lab.infra.repository.ProjectRepository;
import com.seal.seal_lab.infra.repository.PublicationRepository;
import com.seal.seal_lab.infra.repository.TrustOverrideAuditLogRepository;
import com.seal.seal_lab.infra.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "discord.webhook.url="
})
class SealLabApplicationTests {

    @MockitoBean
    private ContactInfoRepository contactInfoRepository;

    @MockitoBean
    private GalleryRepository galleryRepository;

    @MockitoBean
    private LabIntroRepository labIntroRepository;

    @MockitoBean
    private MfaAuditLogRepository mfaAuditLogRepository;

    @MockitoBean
    private MfaBackupCodeRepository mfaBackupCodeRepository;

    @MockitoBean
    private NewsRepository newsRepository;

    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private ProfessorRepository professorRepository;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private PublicationRepository publicationRepository;

    @MockitoBean
    private TrustOverrideAuditLogRepository trustOverrideAuditLogRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }

}
