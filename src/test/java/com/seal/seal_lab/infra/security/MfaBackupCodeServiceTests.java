package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.MfaBackupCode;
import com.seal.seal_lab.infra.repository.MfaBackupCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaBackupCodeServiceTests {

    @Mock
    private MfaBackupCodeRepository mfaBackupCodeRepository;

    @InjectMocks
    private MfaBackupCodeService mfaBackupCodeService;

    private String normalizedHash;

    @BeforeEach
    void setUp() {
        normalizedHash = "1AF8D12CF24261D0326E366DDA1B4E1F24D8D72A72F76D7BA53EE8BE3C001C4C";
    }

    @Test
    void issuesEightPlainCodesAndStoresOnlyHashes() {
        List<String> codes = mfaBackupCodeService.issueNewCodes("admin");

        assertThat(codes).hasSize(8);
        assertThat(codes).allMatch(code -> code.matches("[A-Z2-9]{4}-[A-Z2-9]{4}"));

        ArgumentCaptor<List<MfaBackupCode>> captor = ArgumentCaptor.forClass(List.class);
        verify(mfaBackupCodeRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(8);
        assertThat(captor.getValue()).allMatch(code -> code.getCodeHash() != null && !code.getCodeHash().contains("-"));
    }

    @Test
    void consumeCodeMarksBackupCodeAsUsed() {
        MfaBackupCode backupCode = MfaBackupCode.builder()
                .loginId("admin")
                .codeHash(normalizedHash)
                .createdAt(LocalDateTime.now())
                .build();

        when(mfaBackupCodeRepository.findByLoginIdAndCodeHashAndUsedAtIsNull(any(), any()))
                .thenReturn(Optional.of(backupCode));

        boolean consumed = mfaBackupCodeService.consumeCode("admin", "ABCD-EFGH");

        assertThat(consumed).isTrue();
        assertThat(backupCode.getUsedAt()).isNotNull();
        verify(mfaBackupCodeRepository).save(backupCode);
    }
}
