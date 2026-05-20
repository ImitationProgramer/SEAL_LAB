package com.seal.seal_lab.api.service;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserManagementService adminUserManagementService;

    @Test
    void searchUsersFiltersByLabRank() {
        User generalPublic = User.builder()
                .loginId("public-user")
                .name("일반인")
                .role(User.Role.MEMBER)
                .labRank(User.LabRank.GENERAL_PUBLIC)
                .build();
        User intern = User.builder()
                .loginId("intern-user")
                .name("인턴")
                .role(User.Role.MEMBER)
                .labRank(User.LabRank.INTERN)
                .build();

        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(generalPublic, intern));

        List<User> result = adminUserManagementService.searchUsers(null, null, "INTERN");

        assertThat(result).containsExactly(intern);
    }

    @Test
    void searchUsersRejectsUnsupportedLabRankFilter() {
        assertThatThrownBy(() -> adminUserManagementService.searchUsers(null, null, "WRONG_RANK"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("지원하지 않는 labRank 필터입니다.");
    }

    @Test
    void updateLabRankPersistsResolvedValue() {
        User targetUser = User.builder()
                .loginId("target-user")
                .role(User.Role.MEMBER)
                .labRank(User.LabRank.GENERAL_PUBLIC)
                .build();
        when(userRepository.findByLoginId("target-user")).thenReturn(Optional.of(targetUser));

        adminUserManagementService.updateLabRank("target-user", "MASTER_STUDENT", "admin");

        assertThat(targetUser.getResolvedLabRank()).isEqualTo(User.LabRank.MASTER_STUDENT);
        verify(userRepository).save(targetUser);
    }
}
