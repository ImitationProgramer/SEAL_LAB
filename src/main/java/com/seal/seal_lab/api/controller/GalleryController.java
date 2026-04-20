package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.Gallery;
import com.seal.seal_lab.infra.repository.GalleryRepository;
import com.seal.seal_lab.core.annotation.ZeroTrust; // 어노테이션 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GalleryController {

    private final GalleryRepository galleryRepository;

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

        /**if (!file.isEmpty()) {
            // [중요] JAR 배포 환경을 고려하여 외부 절대 경로를 사용합니다.
            // AWS 환경이라면 "/home/ubuntu/uploads/gallery/" 등으로 설정하세요.
            String uploadDir = "/home/ubuntu/uploads/gallery/";

            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
                log.info("[FILE-SYSTEM] Upload directory created at: {}", uploadDir);
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File destFile = new File(uploadDir, fileName);
            file.transferTo(destFile);

            Gallery gallery = Gallery.builder()
                    .title(title)
                    .imagePath("/uploads/gallery/" + fileName) // DB에는 웹 접근 경로 저장
                    .uploadDate(LocalDateTime.now())
                    .build();

            galleryRepository.save(gallery);
            log.info("[GALLERY-ADD] New image uploaded by admin. Title: {}", title);
        }**/
        if (!file.isEmpty()) {
            // [Local Path] 주현님의 로컬 절대 경로로 설정
            String uploadDir = "/Users/leejoohyun/IdeaProjects/SEAL_LAB/src/main/resources/static/images/";

            File folder = new File(uploadDir);
            if (!folder.exists()) {
                folder.mkdirs();
                log.info("[LOCAL-SYSTEM] 폴더가 없어 생성했습니다: {}", uploadDir);
            }

            // 파일명 중복 방지를 위한 UUID 생성
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File destFile = new File(uploadDir, fileName);
            file.transferTo(destFile);

            // DB 저장 로직
            Gallery gallery = Gallery.builder()
                    .title(title)
                    // static 폴더는 웹상에서 루트(/)로 잡히므로 경로를 아래와 같이 저장합니다.
                    .imagePath("/images/" + fileName)
                    .uploadDate(LocalDateTime.now())
                    .build();

            galleryRepository.save(gallery);
            log.info("[GALLERY-ADD] 로컬 경로 업로드 완료. 점수 검증 통과. Title: {}", title);
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
        galleryRepository.deleteById(id);
        log.warn("[GALLERY-DELETE] Image ID: {} deleted from gallery.", id);
        return "redirect:/gallery";
    }
}