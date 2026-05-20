package com.seal.seal_lab.infra.repository;

import com.seal.seal_lab.core.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    void deleteByLoginIdAndPurposeAndUsedAtIsNull(String loginId, PasswordResetToken.Purpose purpose);
}
