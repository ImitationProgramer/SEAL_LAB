package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.Gallery;
import com.seal.seal_lab.infra.repository.GalleryRepository;
import lombok.RequiredArgsConstructor;
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
public class GalleryController {

    private final GalleryRepository galleryRepository;

    @GetMapping("/gallery")
    public String list(Model model) {
        model.addAttribute("images", galleryRepository.findAllByOrderByUploadDateDesc());
        return "gallery/list";
    }

    @PostMapping("/admin/gallery/add")
    public String add(@RequestParam("title") String title,
                      @RequestParam("file") MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            String projectPath = System.getProperty("user.dir") + "/src/main/resources/static/uploads/gallery/";
            File folder = new File(projectPath);
            if (!folder.exists()) folder.mkdirs();

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            file.transferTo(new File(projectPath, fileName));

            Gallery gallery = Gallery.builder()
                    .title(title)
                    .imagePath("/uploads/gallery/" + fileName)
                    .uploadDate(LocalDateTime.now())
                    .build();
            galleryRepository.save(gallery);
        }
        return "redirect:/gallery";
    }

    @PostMapping("/admin/gallery/delete/{id}")
    public String delete(@PathVariable Long id) {
        galleryRepository.deleteById(id);
        return "redirect:/gallery";
    }
}