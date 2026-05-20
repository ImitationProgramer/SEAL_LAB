package com.seal.seal_lab.api.service;

import com.seal.seal_lab.api.dto.UserProfileUpdateDto;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;

    public User getUserProfile(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + loginId));
    }

    public UserProfileUpdateDto toUpdateDto(User user) {
        UserProfileUpdateDto dto = new UserProfileUpdateDto();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setDepartment(user.getDepartment());
        dto.setKeywords(user.getKeywords());
        dto.setBio(user.getBio());
        return dto;
    }

    public void updateProfile(String loginId,
                              UserProfileUpdateDto dto,
                              MultipartFile file) throws IOException {
        User user = getUserProfile(loginId);

        user.setName(dto.getName());
        user.setEmail(emptyToNull(dto.getEmail()));
        user.setDepartment(emptyToNull(dto.getDepartment()));
        user.setKeywords(emptyToNull(dto.getKeywords()));
        user.setBio(emptyToNull(dto.getBio()));

        if (file != null && !file.isEmpty()) {
            user.setImagePath(storeProfileImage(file));
        }

        userRepository.save(user);
    }

    private String storeProfileImage(MultipartFile file) throws IOException {
        String projectPath = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
        File folder = new File(projectPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        file.transferTo(new File(projectPath, fileName));
        return "/uploads/" + fileName;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
