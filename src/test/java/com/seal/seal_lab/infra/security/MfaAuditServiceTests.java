package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.MfaAuditLog;
import com.seal.seal_lab.infra.repository.MfaAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MfaAuditServiceTests {

    @Mock
    private MfaAuditLogRepository mfaAuditLogRepository;

    @InjectMocks
    private MfaAuditService mfaAuditService;

    @Test
    void stepUpSuccessIsPersistedWithStructuredScoresAndReturnUri() {
        mfaAuditService.recordStepUpSuccess(
                "admin",
                75,
                90,
                90,
                "203.0.113.10",
                "fp:firefox|windows|desktop",
                "/admin/users/member/edit"
        );

        ArgumentCaptor<MfaAuditLog> captor = ArgumentCaptor.forClass(MfaAuditLog.class);
        verify(mfaAuditLogRepository).save(captor.capture());

        MfaAuditLog saved = captor.getValue();
        assertThat(saved.getLoginId()).isEqualTo("admin");
        assertThat(saved.getFlowType()).isEqualTo(MfaAuditLog.FlowType.STEP_UP);
        assertThat(saved.getResultType()).isEqualTo(MfaAuditLog.ResultType.SUCCESS);
        assertThat(saved.getCurrentTrustScore()).isEqualTo(75);
        assertThat(saved.getRequiredTrustScore()).isEqualTo(90);
        assertThat(saved.getGrantedTrustScore()).isEqualTo(90);
        assertThat(saved.getRequestIp()).isEqualTo("203.0.113.10");
        assertThat(saved.getSessionFingerprint()).isEqualTo("fp:firefox|windows|desktop");
        assertThat(saved.getReturnUri()).isEqualTo("/admin/users/member/edit");
    }

    @Test
    void searchAuditLogsDelegatesWithParsedEnumFilters() {
        MfaAuditLog auditLog = MfaAuditLog.builder()
                .loginId("admin")
                .flowType(MfaAuditLog.FlowType.ADMIN_MFA_RESET)
                .resultType(MfaAuditLog.ResultType.SUCCESS)
                .build();
        when(mfaAuditLogRepository.searchAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(auditLog));

        List<MfaAuditLog> result = mfaAuditService.searchAuditLogs(
                "admin",
                "ADMIN_MFA_RESET",
                "SUCCESS",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 19)
        );

        assertThat(result).containsExactly(auditLog);
    }

    @Test
    void searchAuditLogsRejectsUnsupportedFlowTypeFilter() {
        assertThatThrownBy(() -> mfaAuditService.searchAuditLogs("admin", "WRONG_FLOW", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 flowType 필터입니다.");
    }
}
