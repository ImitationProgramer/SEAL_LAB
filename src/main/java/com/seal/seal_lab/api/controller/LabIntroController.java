package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.LabIntro;
import com.seal.seal_lab.infra.repository.LabIntroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class LabIntroController {

    private final LabIntroRepository labIntroRepository;

    /**
     * 연구실 소개글 수정 처리 (AJAX 전용)
     */
    @PostMapping("/admin/intro/edit")
    @ResponseBody
    public ResponseEntity<String> updateIntro(@RequestParam("content") String content) {
        // DB에서 기존 데이터를 찾습니다.
        List<LabIntro> intros = labIntroRepository.findAll();
        LabIntro intro;

        if (intros.isEmpty()) {
            // 혹시 데이터가 없다면 새로 생성합니다.
            intro = LabIntro.builder().content(content).build();
        } else {
            // 기존 데이터가 있다면 내용을 업데이트합니다.
            intro = intros.get(0);
            intro.setContent(content);
        }

        labIntroRepository.save(intro);
        return ResponseEntity.ok("success");
    }
}