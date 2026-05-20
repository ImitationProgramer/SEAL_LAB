package com.seal.seal_lab;

import com.seal.seal_lab.api.dto.TrustDebugView;
import com.seal.seal_lab.core.entity.MfaAuditLog;
import com.seal.seal_lab.core.entity.TrustOverrideAuditLog;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.core.service.ZeroTrustService;
import com.seal.seal_lab.infra.security.MfaAuditService;
import com.seal.seal_lab.infra.security.MfaBackupCodeService;
import com.seal.seal_lab.infra.security.MfaSessionService;
import com.seal.seal_lab.infra.security.PasswordResetService;
import com.seal.seal_lab.infra.security.PasswordSessionService;
import com.seal.seal_lab.infra.repository.ContactInfoRepository;
import com.seal.seal_lab.infra.repository.GalleryRepository;
import com.seal.seal_lab.infra.repository.LabIntroRepository;
import com.seal.seal_lab.infra.repository.NewsRepository;
import com.seal.seal_lab.infra.repository.ProfessorRepository;
import com.seal.seal_lab.infra.repository.ProjectRepository;
import com.seal.seal_lab.infra.repository.PublicationRepository;
import com.seal.seal_lab.infra.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.WebApplicationContext;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "discord.webhook.url="
})
class SecurityAccessFlowMockMvcTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MfaSessionService mfaSessionService;

    @Autowired
    private PasswordSessionService passwordSessionService;

    private MockMvc mockMvc;
    private User adminUser;
    private User memberUser;

    @MockitoBean
    private ZeroTrustService zeroTrustService;

    @MockitoBean
    private MfaAuditService mfaAuditService;

    @MockitoBean
    private MfaBackupCodeService mfaBackupCodeService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ProjectRepository projectRepository;

    @MockitoBean
    private GalleryRepository galleryRepository;

    @MockitoBean
    private LabIntroRepository labIntroRepository;

    @MockitoBean
    private ProfessorRepository professorRepository;

    @MockitoBean
    private ContactInfoRepository contactInfoRepository;

    @MockitoBean
    private PublicationRepository publicationRepository;

    @MockitoBean
    private NewsRepository newsRepository;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        adminUser = User.builder()
                .loginId("admin")
                .password(new BCryptPasswordEncoder().encode("Oldpass1!"))
                .name("관리자")
                .role(User.Role.ADMIN)
                .mfaEnabled(true)
                .mfaSecret("JBSWY3DPEHPK3PXP")
                .trustScore(100)
                .build();
        memberUser = User.builder()
                .loginId("member")
                .password(new BCryptPasswordEncoder().encode("Oldpass1!"))
                .name("일반회원")
                .role(User.Role.MEMBER)
                .trustScore(100)
                .build();

        when(userRepository.findByLoginId(anyString())).thenReturn(Optional.of(adminUser));
        when(userRepository.findByLoginId("member")).thenReturn(Optional.of(memberUser));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotAccessAdminUserEditPage() throws Exception {
        mockMvc.perform(get("/admin/users/admin/edit"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"))
                .andExpect(request().attribute("denialType", "ROLE"))
                .andExpect(request().attribute("errorMessage", "관리자 권한이 필요한 기능입니다."));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotAccessProjectsAdminEndpointWithDirectUrl() throws Exception {
        mockMvc.perform(post("/projects/admin/delete/1"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"))
                .andExpect(request().attribute("denialType", "ROLE"))
                .andExpect(request().attribute("errorMessage", "관리자 권한이 필요한 기능입니다."));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithLowTrustScoreIsBlockedByZeroTrust() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(80);

        mockMvc.perform(get("/admin/users/member/edit"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/access-denied"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("보안 요구 수준을 충족하지 못했습니다.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("80점")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MFA 추가 인증으로 계속하기")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithEnoughTrustScoreCanAccessAdminUserEditPage() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);

        mockMvc.perform(get("/admin/users/member/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanStartStepUpAfterTrustDeniedPageStoresCandidateInSession() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(80);

        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/admin/users/member/edit"))
                .andExpect(status().isForbidden())
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(post("/mfa/step-up/start").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa/verify"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithValidStepUpGrantCanAccessAdminUserEditPageEvenWhenBaseScoreIsLow() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mfaSessionService.grantStepUp(
                session,
                "admin",
                90,
                "127.0.0.1",
                "fp:unknown|unknown|unknown",
                Duration.ofMinutes(10)
        );

        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(80);

        mockMvc.perform(get("/admin/users/member/edit").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));
    }

    @Test
    void anonymousUserIsRedirectedToLoginForTrustDebugPage() throws Exception {
        mockMvc.perform(get("/trust/debug"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void anonymousUserCanOpenPasswordForgotPage() throws Exception {
        mockMvc.perform(get("/password/forgot"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/password_forgot"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("관리자 비밀번호 재설정 요청")));
    }

    @Test
    void forgotPasswordRequestShowsIssuedTokenPage() throws Exception {
        when(passwordResetService.issueAdminResetToken("admin", "admin@example.com"))
                .thenReturn(new PasswordResetService.IssuedResetToken(
                        "admin",
                        "ABCD2345EFGH6789JKLM2345",
                        java.time.LocalDateTime.now().plusMinutes(15)
                ));

        mockMvc.perform(post("/password/forgot")
                        .param("loginId", "admin")
                        .param("email", "admin@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/password_reset_requested"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ABCD2345EFGH6789JKLM2345")));
    }

    @Test
    void anonymousUserCanOpenMemberPasswordForgotPage() throws Exception {
        mockMvc.perform(get("/password/forgot/member"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/member_password_forgot"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("회원 비밀번호 재설정 요청")));
    }

    @Test
    void memberForgotPasswordRequestShowsIssuedTokenPage() throws Exception {
        when(passwordResetService.issueMemberResetToken("member", "member@example.com"))
                .thenReturn(new PasswordResetService.IssuedResetToken(
                        "member",
                        "MNOP2345QRST6789UVWX2345",
                        java.time.LocalDateTime.now().plusMinutes(15)
                ));

        mockMvc.perform(post("/password/forgot/member")
                        .param("loginId", "member")
                        .param("email", "member@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/member_password_reset_requested"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MNOP2345QRST6789UVWX2345")));
    }

    @Test
    void validResetTokenRendersPasswordResetPage() throws Exception {
        when(passwordResetService.validateResetToken("ABCD2345EFGH6789JKLM2345"))
                .thenReturn(new PasswordResetService.ResetTokenPreview(
                        "admin",
                        java.time.LocalDateTime.now().plusMinutes(15)
                ));

        mockMvc.perform(get("/password/reset").param("token", "ABCD2345EFGH6789JKLM2345"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/password_reset"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("backup code")));
    }

    @Test
    void validMemberResetTokenRendersMemberPasswordResetPage() throws Exception {
        when(passwordResetService.validateMemberResetToken("MNOP2345QRST6789UVWX2345"))
                .thenReturn(new PasswordResetService.ResetTokenPreview(
                        "member",
                        java.time.LocalDateTime.now().plusMinutes(15)
                ));

        mockMvc.perform(get("/password/reset/member").param("token", "MNOP2345QRST6789UVWX2345"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/member_password_reset"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("backup code"))));
    }

    @Test
    void successfulPasswordResetRedirectsToLogin() throws Exception {
        when(passwordResetService.validateResetToken("ABCD2345EFGH6789JKLM2345"))
                .thenReturn(new PasswordResetService.ResetTokenPreview(
                        "admin",
                        java.time.LocalDateTime.now().plusMinutes(15)
                ));

        mockMvc.perform(post("/password/reset")
                        .param("token", "ABCD2345EFGH6789JKLM2345")
                        .param("backupCode", "ABCD-EFGH")
                        .param("newPassword", "Newpass2!")
                        .param("confirmPassword", "Newpass2!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordReset=true"));

        verify(passwordResetService).resetAdminPassword("ABCD2345EFGH6789JKLM2345", "ABCD-EFGH", "Newpass2!", "Newpass2!");
    }

    @Test
    void successfulMemberPasswordResetRedirectsToLogin() throws Exception {
        when(passwordResetService.validateMemberResetToken("MNOP2345QRST6789UVWX2345"))
                .thenReturn(new PasswordResetService.ResetTokenPreview(
                        "member",
                        java.time.LocalDateTime.now().plusMinutes(15)
                ));

        mockMvc.perform(post("/password/reset/member")
                        .param("token", "MNOP2345QRST6789UVWX2345")
                        .param("newPassword", "Newpass2!")
                        .param("confirmPassword", "Newpass2!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordReset=true"));

        verify(passwordResetService).resetMemberPassword("MNOP2345QRST6789UVWX2345", "Newpass2!", "Newpass2!");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPendingLoginVerificationIsRedirectedToMfaVerify() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mfaSessionService.beginLoginVerification(session, "admin", false);

        mockMvc.perform(get("/trust/debug").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa/verify"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanFetchMfaSetupQrImage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mfaSessionService.beginLoginVerification(session, "admin", true);

        mockMvc.perform(get("/mfa/setup/qr").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanOpenBackupCodeRecoveryPageDuringPendingLoginVerification() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mfaSessionService.beginLoginVerification(session, "admin", false);

        mockMvc.perform(get("/mfa/recovery").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/mfa_recovery"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Backup Code Recovery")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanOpenPasswordChangePage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        passwordSessionService.markAuthenticationEstablished(session);

        mockMvc.perform(get("/admin/security/password").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/password_change"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("관리자 비밀번호 변경")));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCanOpenPasswordChangePage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        passwordSessionService.markAuthenticationEstablished(session);

        mockMvc.perform(get("/member/security/password").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/member_password_change"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("회원 비밀번호 변경")));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberPasswordChangeRedirectsToLoginAndInvalidatesCurrentSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        passwordSessionService.markAuthenticationEstablished(session);

        mockMvc.perform(post("/member/security/password")
                        .session(session)
                        .param("currentPassword", "Oldpass1!")
                        .param("newPassword", "Newpass2!")
                        .param("confirmPassword", "Newpass2!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordChanged=true"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanResetTargetAdminsMfaFromTrustDebugTools() throws Exception {
        MockHttpSession session = new MockHttpSession();
        passwordSessionService.markAuthenticationEstablished(session);
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(100);

        mockMvc.perform(post("/admin/mfa/reset")
                        .session(session)
                        .param("loginId", "admin")
                        .param("reason", "backup code 소진 및 기기 분실"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trust/debug?loginId=admin"));

        verify(mfaBackupCodeService).clearAllCodes("admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithMfaDisabledIsForcedBackToSetupPage() throws Exception {
        adminUser.setMfaEnabled(false);

        mockMvc.perform(get("/trust/debug"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa/setup"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminPasswordChangeRedirectsToLoginAndInvalidatesCurrentSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        passwordSessionService.markAuthenticationEstablished(session);

        mockMvc.perform(post("/admin/security/password")
                        .session(session)
                        .param("currentPassword", "Oldpass1!")
                        .param("newPassword", "Newpass2!")
                        .param("confirmPassword", "Newpass2!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?passwordChanged=true"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void staleSessionIsRevokedAfterPasswordChangeTimestampMovesForward() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("security.authEstablishedAt", java.time.LocalDateTime.now().minusMinutes(10));
        adminUser.setPasswordChangedAt(java.time.LocalDateTime.now());

        mockMvc.perform(get("/trust/debug").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?sessionRevoked=true"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanViewBackupCodeStatusAfterMfaVerification() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mfaSessionService.beginLoginVerification(session, "admin", false);
        mfaSessionService.markLoginVerified(session);
        when(mfaBackupCodeService.countRemainingCodes("admin")).thenReturn(3L);

        mockMvc.perform(get("/mfa/recovery/status").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/mfa_recovery_status"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("남은 backup code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">3<")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanReissueBackupCodesAndSeeOneTimeDisplayPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mfaSessionService.beginLoginVerification(session, "admin", false);
        mfaSessionService.markLoginVerified(session);
        when(mfaBackupCodeService.countRemainingCodes("admin")).thenReturn(0L);
        when(mfaBackupCodeService.issueNewCodes("admin")).thenReturn(List.of(
                "ABCD-EFGH",
                "JKLM-NPQR",
                "STUV-WXYZ",
                "2345-6789",
                "BCDF-GHJK",
                "LMNP-QRST",
                "UVWX-YZ23",
                "4567-89AB"
        ));

        MockHttpSession reissueSession = (MockHttpSession) mockMvc.perform(post("/mfa/recovery/reissue").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mfa/recovery/codes?source=reissue"))
                .andReturn()
                .getRequest()
                .getSession(false);

        mockMvc.perform(get("/mfa/recovery/codes").param("source", "reissue").session(reissueSession))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/mfa_recovery_codes"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("기존 미사용 backup code는 모두 폐기")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ABCD-EFGH")));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotAccessTrustDebugPage() throws Exception {
        mockMvc.perform(get("/trust/debug"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"))
                .andExpect(request().attribute("denialType", "ROLE"));

        verify(zeroTrustService, never()).inspectCurrentTrustStatus(eq("member"), anyString(), isNull());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void trustDebugUsesForwardedClientIpWhenProxyHeadersExist() throws Exception {
        when(zeroTrustService.inspectCurrentTrustStatus("admin", "203.0.113.10", null)).thenReturn(
                liveTrustDebugView("admin", "ADMIN", 88, 88, false)
        );

        mockMvc.perform(get("/trust/debug")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.5"))
                .andExpect(status().isOk())
                .andExpect(view().name("trust/debug"));

        verify(zeroTrustService).inspectCurrentTrustStatus(eq("admin"), eq("203.0.113.10"), isNull());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanLookupStoredTrustStatusOfAnotherUser() throws Exception {
        when(zeroTrustService.inspectCurrentTrustStatus("admin", "127.0.0.1", null)).thenReturn(
                liveTrustDebugView("admin", "ADMIN", 95, 95, true)
        );
        when(zeroTrustService.inspectStoredTrustStatus("target-user")).thenReturn(
                TrustDebugView.builder()
                        .loginId("target-user")
                        .role("MEMBER")
                        .storedTrustScore(72)
                        .evaluatedTrustScore(null)
                        .recoveredTrustScore(74)
                        .recoveredPoints(2)
                        .minutesSinceLastAccess(25)
                        .devicePenalty(0)
                        .deviceReason("조회 전용")
                        .impossibleTravelPenalty(0)
                        .impossibleTravelReason("조회 전용")
                        .currentIp(null)
                        .currentFingerprint(null)
                        .registeredFingerprint("fp:firefox|windows|desktop")
                        .currentLocation("위치 정보 없음")
                        .lastLocation("37.2758, 127.1325")
                        .lastAccessTime("2026-05-18T11:00:00")
                        .riskLevel("Caution")
                        .adminActionAllowed(false)
                        .liveEvaluation(false)
                        .build()
        );

        mockMvc.perform(get("/trust/debug").param("loginId", "target-user"))
                .andExpect(status().isOk())
                .andExpect(view().name("trust/debug"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("조회 대상: target-user")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fp:firefox|windows|desktop")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void lowTrustAdminCanStillOpenTrustDebugPageForSelfDiagnosis() throws Exception {
        when(zeroTrustService.inspectCurrentTrustStatus("admin", "127.0.0.1", null)).thenReturn(
                liveTrustDebugView("admin", "ADMIN", 18, 18, false)
        );

        mockMvc.perform(get("/trust/debug"))
                .andExpect(status().isOk())
                .andExpect(view().name("trust/debug"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("18")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Admin Action Blocked")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithEnoughTrustScoreCanDeleteProject() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);

        mockMvc.perform(post("/projects/admin/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/projects"));

        verify(projectRepository).deleteById(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void zeroTrustAspectUsesForwardedClientIpWhenProxyHeadersExist() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "198.51.100.24", null)).thenReturn(95);

        mockMvc.perform(get("/admin/users/member/edit")
                        .header("X-Forwarded-For", "198.51.100.24, 10.10.10.10"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"));

        verify(zeroTrustService).calculateTrustScore("admin", "198.51.100.24", null);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithInsufficientTrustScoreCannotDeleteProject() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(94);

        mockMvc.perform(post("/projects/admin/delete/1"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/access-denied"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("94점")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("요구 점수(95)")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithEnoughTrustScoreCanOverrideTrustState() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);

        mockMvc.perform(post("/admin/trust/override")
                        .param("loginId", "target-user")
                        .param("trustScore", "100")
                        .param("clearContext", "true")
                        .param("reason", "실검증 전 점수 복구"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trust/debug?loginId=target-user"))
                .andExpect(flash().attribute("operationMessage",
                        "'target-user' 계정 점수를 100점으로 적용했습니다. 저장된 위치와 fingerprint 컨텍스트도 초기화했습니다."));

        verify(zeroTrustService).overrideTrustState(
                "target-user", 100, true, "실검증 전 점수 복구",
                "127.0.0.1", "fp:unknown|unknown|unknown", "admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithInsufficientTrustScoreCannotOverrideTrustState() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(94);

        mockMvc.perform(post("/admin/trust/override")
                        .param("loginId", "target-user")
                        .param("trustScore", "100")
                        .param("reason", "점수 조정 테스트"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/access-denied"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("요구 점수(95)")));

        verify(zeroTrustService, never()).overrideTrustState(
                "target-user", 100, false, "점수 조정 테스트",
                "127.0.0.1", "fp:unknown|unknown|unknown", "admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminGetsValidationErrorWhenOverrideScoreIsOutOfRange() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        doThrow(new IllegalArgumentException("신뢰 점수는 0점에서 100점 사이여야 합니다."))
                .when(zeroTrustService).overrideTrustState(
                        "target-user", 101, false, "잘못된 점수 입력",
                        "127.0.0.1", "fp:unknown|unknown|unknown", "admin");

        mockMvc.perform(post("/admin/trust/override")
                        .param("loginId", "target-user")
                        .param("trustScore", "101")
                        .param("reason", "잘못된 점수 입력"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trust/debug?loginId=target-user"))
                .andExpect(flash().attribute("operationError", "신뢰 점수는 0점에서 100점 사이여야 합니다."));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminGetsNotFoundErrorWhenOverrideTargetDoesNotExist() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        doThrow(new NoSuchElementException("대상 사용자를 찾을 수 없습니다: missing-user"))
                .when(zeroTrustService).overrideTrustState(
                        "missing-user", 80, false, "없는 계정 점검",
                        "127.0.0.1", "fp:unknown|unknown|unknown", "admin");

        mockMvc.perform(post("/admin/trust/override")
                        .param("loginId", "missing-user")
                        .param("trustScore", "80")
                        .param("reason", "없는 계정 점검"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trust/debug?loginId=missing-user"))
                .andExpect(flash().attribute("operationError", "대상 사용자를 찾을 수 없습니다: missing-user"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminGetsValidationErrorWhenOverrideReasonIsBlank() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        doThrow(new IllegalArgumentException("운영 사유는 비워둘 수 없습니다."))
                .when(zeroTrustService).overrideTrustState(
                        "target-user", 90, false, "",
                        "127.0.0.1", "fp:unknown|unknown|unknown", "admin");

        mockMvc.perform(post("/admin/trust/override")
                        .param("loginId", "target-user")
                        .param("trustScore", "90")
                        .param("reason", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trust/debug?loginId=target-user"))
                .andExpect(flash().attribute("operationError", "운영 사유는 비워둘 수 없습니다."));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithEnoughTrustScoreCanApplyTestBaseline() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);

        mockMvc.perform(post("/admin/trust/test-baseline")
                        .param("loginId", "jspark")
                        .param("reason", "브라우저 테스트 초기화"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/trust/debug?loginId=jspark"))
                .andExpect(flash().attribute("operationMessage",
                        "'jspark' 계정을 테스트 기준선으로 초기화했습니다. 100점, Windows 데스크톱 Chrome fingerprint, 위치/시간 초기화가 적용됩니다."));

        verify(zeroTrustService).resetTrustStateToTestBaseline(
                "jspark", "브라우저 테스트 초기화",
                "127.0.0.1", "fp:unknown|unknown|unknown", "admin");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithInsufficientTrustScoreCannotApplyTestBaseline() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(94);

        mockMvc.perform(post("/admin/trust/test-baseline")
                        .param("loginId", "jspark")
                        .param("reason", "기준선 재설정"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/access-denied"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("요구 점수(95)")));

        verify(zeroTrustService, never()).resetTrustStateToTestBaseline(
                "jspark", "기준선 재설정",
                "127.0.0.1", "fp:unknown|unknown|unknown", "admin");
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotAccessTrustAuditPage() throws Exception {
        mockMvc.perform(get("/admin/trust/audit"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"))
                .andExpect(request().attribute("denialType", "ROLE"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithEnoughTrustScoreCanAccessTrustAuditPage() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        when(zeroTrustService.searchTrustOverrideLogs(null, null, null, null, null, null)).thenReturn(List.of(
                TrustOverrideAuditLog.builder()
                        .operatorLoginId("admin")
                        .targetLoginId("jspark")
                        .previousTrustScore(75)
                        .appliedTrustScore(100)
                        .actionType(TrustOverrideAuditLog.ActionType.TEST_BASELINE_RESET)
                        .clearContext(true)
                        .reason("브라우저 테스트 초기화")
                        .previousFingerprint("fp:chrome|windows|desktop")
                        .build()
        ));

        mockMvc.perform(get("/admin/trust/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("trust/audit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trust Override Audit")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("75 -&gt; 100")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TEST BASELINE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("브라우저 테스트 초기화")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanFilterTrustAuditPageByOperatorTargetActionAndDateRange() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        when(zeroTrustService.searchTrustOverrideLogs("admin", "jspark", "MANUAL_OVERRIDE", "PRESERVED",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18))).thenReturn(List.of(
                TrustOverrideAuditLog.builder()
                        .operatorLoginId("admin")
                        .targetLoginId("jspark")
                        .previousTrustScore(90)
                        .appliedTrustScore(100)
                        .actionType(TrustOverrideAuditLog.ActionType.MANUAL_OVERRIDE)
                        .clearContext(false)
                        .reason("관리자 요청 대응")
                        .build()
        ));

        mockMvc.perform(get("/admin/trust/audit")
                        .param("operator", "admin")
                        .param("target", "jspark")
                        .param("actionType", "MANUAL_OVERRIDE")
                        .param("contextReset", "PRESERVED")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-18"))
                .andExpect(status().isOk())
                .andExpect(view().name("trust/audit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("90 -&gt; 100")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MANUAL OVERRIDE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Preserved")));

        verify(zeroTrustService).searchTrustOverrideLogs("admin", "jspark", "MANUAL_OVERRIDE", "PRESERVED",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidDateRangeShowsAuditFilterError() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        doThrow(new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다."))
                .when(zeroTrustService).searchTrustOverrideLogs("admin", null, null, null,
                        LocalDate.of(2026, 5, 19), LocalDate.of(2026, 5, 18));

        mockMvc.perform(get("/admin/trust/audit")
                        .param("operator", "admin")
                        .param("dateFrom", "2026-05-19")
                        .param("dateTo", "2026-05-18"))
                .andExpect(status().isOk())
                .andExpect(view().name("trust/audit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("시작일은 종료일보다 늦을 수 없습니다.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("검색 조건에 맞는 override 감사 이력이 없습니다.")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidActionTypeShowsAuditFilterError() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        doThrow(new IllegalArgumentException("지원하지 않는 actionType 필터입니다."))
                .when(zeroTrustService).searchTrustOverrideLogs(null, null, "WRONG_TYPE", null, null, null);

        mockMvc.perform(get("/admin/trust/audit")
                        .param("actionType", "WRONG_TYPE"))
                .andExpect(status().isOk())
                .andExpect(view().name("trust/audit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("지원하지 않는 actionType 필터입니다.")));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotAccessSecurityAuditPage() throws Exception {
        mockMvc.perform(get("/admin/security/audit"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"))
                .andExpect(request().attribute("denialType", "ROLE"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithInsufficientTrustScoreCannotAccessSecurityAuditPage() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(89);

        mockMvc.perform(get("/admin/security/audit"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/access-denied"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("요구 점수(90)")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithEnoughTrustScoreCanAccessSecurityAuditPage() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        when(mfaAuditService.searchAuditLogs(null, null, null, null, null)).thenReturn(List.of(
                MfaAuditLog.builder()
                        .loginId("admin")
                        .flowType(MfaAuditLog.FlowType.ADMIN_MFA_RESET)
                        .resultType(MfaAuditLog.ResultType.SUCCESS)
                        .requestIp("203.0.113.10")
                        .sessionFingerprint("fp:chrome|windows|desktop")
                        .detailMessage("target=target-admin,reason=기기 분실")
                        .build()
        ));

        mockMvc.perform(get("/admin/security/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("security/audit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Security Audit")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ADMIN_MFA_RESET")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("target=target-admin,reason=기기 분실")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanFilterSecurityAuditPage() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        when(mfaAuditService.searchAuditLogs(
                "admin",
                "ADMIN_PASSWORD_RESET_COMPLETE",
                "SUCCESS",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 19)
        )).thenReturn(List.of(
                MfaAuditLog.builder()
                        .loginId("admin")
                        .flowType(MfaAuditLog.FlowType.ADMIN_PASSWORD_RESET_COMPLETE)
                        .resultType(MfaAuditLog.ResultType.SUCCESS)
                        .detailMessage("backup code verified")
                        .build()
        ));

        mockMvc.perform(get("/admin/security/audit")
                        .param("loginId", "admin")
                        .param("flowType", "ADMIN_PASSWORD_RESET_COMPLETE")
                        .param("resultType", "SUCCESS")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-19"))
                .andExpect(status().isOk())
                .andExpect(view().name("security/audit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ADMIN_PASSWORD_RESET_COMPLETE")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("backup code verified")));

        verify(mfaAuditService).searchAuditLogs(
                "admin",
                "ADMIN_PASSWORD_RESET_COMPLETE",
                "SUCCESS",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 19)
        );
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotAccessAdminUserManagementPage() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"))
                .andExpect(request().attribute("denialType", "ROLE"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithEnoughTrustScoreCanAccessAdminUserManagementPage() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        when(userRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(
                adminUser,
                memberUser
        ));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User Management")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("일반인")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithInsufficientTrustScoreCannotUpdateLabRank() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(94);

        mockMvc.perform(post("/admin/users/member/lab-rank")
                        .param("labRank", "INTERN"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/access-denied"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("요구 점수(95)")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanUpdateUserLabRank() throws Exception {
        User targetUser = User.builder()
                .loginId("target-user")
                .password(new BCryptPasswordEncoder().encode("Oldpass1!"))
                .name("대상 사용자")
                .email("target@example.com")
                .role(User.Role.MEMBER)
                .labRank(User.LabRank.GENERAL_PUBLIC)
                .trustScore(100)
                .build();

        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(95);
        when(userRepository.findByLoginId("target-user")).thenReturn(Optional.of(targetUser));

        mockMvc.perform(post("/admin/users/target-user/lab-rank")
                        .param("labRank", "INTERN")
                        .param("keyword", "target")
                        .param("role", "MEMBER")
                        .param("currentLabRank", "GENERAL_PUBLIC"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?keyword=target&role=MEMBER&labRank=GENERAL_PUBLIC"))
                .andExpect(flash().attribute("userOperationMessage",
                        "'target-user' 계정의 연구실 직급을 변경했습니다."));

        verify(userRepository).save(targetUser);
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberWithEnoughTrustScoreCanOpenOwnProfileEditPage() throws Exception {
        when(zeroTrustService.calculateTrustScore("member", "127.0.0.1", null)).thenReturn(75);

        mockMvc.perform(get("/profile/me/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("내 공개 프로필 편집")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("일반인")));
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCanUpdateOwnProfile() throws Exception {
        when(zeroTrustService.calculateTrustScore("member", "127.0.0.1", null)).thenReturn(75);

        mockMvc.perform(post("/profile/me/edit")
                        .param("name", "수정된 회원")
                        .param("email", "updated@example.com")
                        .param("department", "AI Security")
                        .param("keywords", "Zero Trust, Malware")
                        .param("bio", "자기소개"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/me/edit"))
                .andExpect(flash().attribute("profileMessage", "내 프로필을 업데이트했습니다."));

        verify(userRepository).save(memberUser);
    }

    @Test
    @WithMockUser(username = "member", roles = "MEMBER")
    void memberCannotAccessAdminUserProfileEditPage() throws Exception {
        mockMvc.perform(get("/admin/users/admin/edit"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/access-denied"))
                .andExpect(request().attribute("denialType", "ROLE"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminWithEnoughTrustScoreCanOpenTargetUserProfileEditPage() throws Exception {
        when(zeroTrustService.calculateTrustScore("admin", "127.0.0.1", null)).thenReturn(90);

        mockMvc.perform(get("/admin/users/member/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("사용자 공개 프로필 편집")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("일반회원")));
    }

    private TrustDebugView liveTrustDebugView(String loginId, String role, int storedScore, int evaluatedScore,
                                              boolean adminActionAllowed) {
        return TrustDebugView.builder()
                .loginId(loginId)
                .role(role)
                .storedTrustScore(storedScore)
                .evaluatedTrustScore(evaluatedScore)
                .recoveredTrustScore(storedScore)
                .recoveredPoints(0)
                .minutesSinceLastAccess(5)
                .devicePenalty(0)
                .deviceReason("현재 등록된 기기와 동일합니다.")
                .impossibleTravelPenalty(0)
                .impossibleTravelReason("근거리 이동 또는 짧은 요청 간격으로 판단되어 예외 처리되었습니다.")
                .currentIp("127.0.0.1")
                .currentFingerprint("fp:chrome|windows|desktop")
                .registeredFingerprint("fp:chrome|windows|desktop")
                .currentLocation("37.2758, 127.1325")
                .lastLocation("37.2758, 127.1325")
                .lastAccessTime("2026-05-18T11:30:00")
                .riskLevel(adminActionAllowed ? "Operational" : "Restricted")
                .adminActionAllowed(adminActionAllowed)
                .liveEvaluation(true)
                .build();
    }
}
