package com.seal.seal_lab.api.service;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminUserManagementService {

    private final UserRepository userRepository;

    public List<User> searchUsers(String loginId, String roleFilter, String labRankFilter) {
        String normalizedLoginId = normalizeFilter(loginId);
        User.Role normalizedRole = normalizeRoleFilter(roleFilter);
        User.LabRank normalizedLabRank = normalizeLabRankFilter(labRankFilter);

        return userRepository.findAll(Sort.by(
                        Sort.Order.asc("role"),
                        Sort.Order.asc("loginId")))
                .stream()
                .filter(user -> normalizedLoginId == null
                        || user.getLoginId().toLowerCase(Locale.ROOT).contains(normalizedLoginId.toLowerCase(Locale.ROOT))
                        || (user.getName() != null
                        && user.getName().toLowerCase(Locale.ROOT).contains(normalizedLoginId.toLowerCase(Locale.ROOT))))
                .filter(user -> normalizedRole == null || user.getRole() == normalizedRole)
                .filter(user -> normalizedLabRank == null || user.getResolvedLabRank() == normalizedLabRank)
                .toList();
    }

    @Transactional
    public void updateLabRank(String targetLoginId, String labRankValue, String operatorLoginId) {
        if (targetLoginId == null || targetLoginId.isBlank()) {
            throw new IllegalArgumentException("대상 loginId는 비워둘 수 없습니다.");
        }

        User.LabRank targetLabRank = normalizeLabRankFilter(labRankValue);
        if (targetLabRank == null) {
            throw new IllegalArgumentException("변경할 직급을 선택해주세요.");
        }

        User user = userRepository.findByLoginId(targetLoginId.trim())
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다: " + targetLoginId));

        User.LabRank previousRank = user.getResolvedLabRank();
        user.setLabRank(targetLabRank);
        userRepository.save(user);

        log.warn("[ADMIN-USER-RANK] Operator: {} | Target: {} | Rank: {} -> {}",
                operatorLoginId, user.getLoginId(), previousRank.name(), targetLabRank.name());
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User.Role normalizeRoleFilter(String roleFilter) {
        String normalized = normalizeFilter(roleFilter);
        if (normalized == null) {
            return null;
        }
        try {
            return User.Role.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 role 필터입니다.");
        }
    }

    private User.LabRank normalizeLabRankFilter(String labRankFilter) {
        String normalized = normalizeFilter(labRankFilter);
        if (normalized == null) {
            return null;
        }
        try {
            return User.LabRank.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 labRank 필터입니다.");
        }
    }
}
