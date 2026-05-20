package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.Gallery;
import com.seal.seal_lab.infra.repository.GalleryRepository;
import com.seal.seal_lab.core.annotation.ZeroTrust; // 어노테이션 임포트
import com.seal.seal_lab.infra.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GalleryController {

    private final GalleryRepository galleryRepository;
    private final S3StorageService s3StorageService;

    /**
     * 갤러리 목록 조회
     * 누구나 볼 수 있어야 하므로 필수 점수를 0점으로 설정합니다.
     */
    @GetMapping("/gallery")
    @ZeroTrust(requiredScore = 0)
    public String list(Model model) {
        model.addAttribute("images", galleryRepository.findAllByOrderByUploadDateDesc());
        return "gallery/list";
    }

    /**
     * 사진 업로드 (관리자 전용)
     * 파일 수정 권한이므로 높은 신뢰 점수(90점)를 요구합니다.
     */
    @PostMapping("/admin/gallery/add")
    @ZeroTrust(requiredScore = 90)
    public String add(@RequestParam("title") String title,
                      @RequestParam("file") MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            Gallery gallery = Gallery.builder()
                    .title(title)
                    .imagePath(s3StorageService.uploadGalleryImage(file))
                    .uploadDate(LocalDateTime.now())
                    .build();

            galleryRepository.save(gallery);
            log.info("[GALLERY-ADD] S3 업로드 완료. 점수 검증 통과. Title: {}", title);
        }
        return "redirect:/gallery";
    }

    /**
     * 사진 삭제 (관리자 전용)
     * 데이터 삭제는 가장 위험한 작업이므로 최고 수준의 보안(95점)을 요구합니다.
     */
    @PostMapping("/admin/gallery/delete/{id}")
    @ZeroTrust(requiredScore = 95)
    public String delete(@PathVariable Long id) {
        galleryRepository.findById(id)
                .map(Gallery::getImagePath)
                .ifPresent(s3StorageService::deleteByUrl);
        galleryRepository.deleteById(id);
        log.warn("[GALLERY-DELETE] Image ID: {} deleted from gallery.", id);
        return "redirect:/gallery";
    }
}
