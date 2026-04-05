package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust; // 어노테이션 임포트
import com.seal.seal_lab.core.entity.LabIntro;
import com.seal.seal_lab.infra.repository.LabIntroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j // 보안 로그 기록을 위해 추가
public class LabIntroController {

    private final LabIntroRepository labIntroRepository;

    /**
     * 연구실 소개글 수정 처리 (AJAX 전용)
     * 메인 페이지의 핵심 정보를 변경하므로 신뢰 점수 90점을 요구합니다.
     */
    @PostMapping("/admin/intro/edit")
    @ResponseBody
    @ZeroTrust(requiredScore = 90) // 제로 트러스트 검증 추가
    public ResponseEntity<String> updateIntro(@RequestParam("content") String content) {

        // [LOG] 수정 시도 기록
        log.info("[ZTA-AUDIT] 연구실 소개글 수정 시도됨. (Required Score: 90)");

        // DB에서 기존 데이터를 찾습니다.
        List<LabIntro> intros = labIntroRepository.findAll();
        LabIntro intro;

        if (intros.isEmpty()) {
            // 데이터가 없다면 새로 생성
            intro = LabIntro.builder().content(content).build();
            log.info("[ZTA-INFO] 신규 소개글 데이터 생성됨.");
        } else {
            // 기존 데이터가 있다면 업데이트
            intro = intros.get(0);
            intro.setContent(content);
            log.info("[ZTA-INFO] 기존 소개글 데이터 업데이트됨.");
        }

        labIntroRepository.save(intro);

        // [LOG] 최종 성공 기록
        log.info("[ZTA-SUCCESS] 연구실 소개글 수정 완료. (User verified via Zero Trust Policy)");

        return ResponseEntity.ok("success");
    }
}