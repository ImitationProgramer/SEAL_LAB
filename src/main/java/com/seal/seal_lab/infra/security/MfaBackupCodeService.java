package com.seal.seal_lab.infra.security;

import com.seal.seal_lab.core.entity.MfaBackupCode;
import com.seal.seal_lab.infra.repository.MfaBackupCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MfaBackupCodeService {

    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int DEFAULT_CODE_COUNT = 8;
    private static final int CODE_LENGTH = 8;

    private final MfaBackupCodeRepository mfaBackupCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public List<String> issueNewCodes(String loginId) {
        return issueNewCodes(loginId, DEFAULT_CODE_COUNT);
    }

    @Transactional
    public List<String> issueNewCodes(String loginId, int count) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 비워둘 수 없습니다.");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("backup code 개수는 1개 이상이어야 합니다.");
        }

        mfaBackupCodeRepository.deleteByLoginId(loginId);

        List<String> plainCodes = new ArrayList<>(count);
        List<MfaBackupCode> backupCodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String plainCode = generatePlainCode();
            plainCodes.add(plainCode);
            backupCodes.add(MfaBackupCode.builder()
                    .loginId(loginId)
                    .codeHash(hash(normalize(plainCode)))
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        mfaBackupCodeRepository.saveAll(backupCodes);
        return plainCodes;
    }

    @Transactional
    public boolean consumeCode(String loginId, String rawCode) {
        if (loginId == null || loginId.isBlank() || rawCode == null || rawCode.isBlank()) {
            return false;
        }

        Optional<MfaBackupCode> backupCode = mfaBackupCodeRepository.findByLoginIdAndCodeHashAndUsedAtIsNull(
                loginId,
                hash(normalize(rawCode))
        );
        if (backupCode.isEmpty()) {
            return false;
        }

        MfaBackupCode entity = backupCode.get();
        entity.setUsedAt(LocalDateTime.now());
        mfaBackupCodeRepository.save(entity);
        return true;
    }

    @Transactional(readOnly = true)
    public long countRemainingCodes(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return 0;
        }
        return mfaBackupCodeRepository.countByLoginIdAndUsedAtIsNull(loginId);
    }

    @Transactional
    public void clearAllCodes(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 비워둘 수 없습니다.");
        }
        mfaBackupCodeRepository.deleteByLoginId(loginId);
    }

    String normalize(String rawCode) {
        return rawCode.replace("-", "").replace(" ", "").trim().toUpperCase(Locale.ROOT);
    }

    private String generatePlainCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH + 1);
        for (int i = 0; i < CODE_LENGTH; i++) {
            if (i == 4) {
                builder.append('-');
            }
            builder.append(CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)]);
        }
        return builder.toString();
    }

    private String hash(String normalizedCode) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("backup code hash 생성에 실패했습니다.", e);
        }
    }
}
