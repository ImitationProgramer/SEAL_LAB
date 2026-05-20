package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.MfaBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, Long> {

    void deleteByLoginId(String loginId);

    Optional<MfaBackupCode> findByLoginIdAndCodeHashAndUsedAtIsNull(String loginId, String codeHash);

    long countByLoginIdAndUsedAtIsNull(String loginId);
}
