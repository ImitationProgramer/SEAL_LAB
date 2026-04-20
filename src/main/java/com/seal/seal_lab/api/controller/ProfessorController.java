package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust; // 어노테이션 임포트
import com.seal.seal_lab.core.entity.Professor;
import com.seal.seal_lab.infra.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 보안 로그 기록을 위해 추가
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfessorController {

    private final ProfessorRepository professorRepository;

    /**
     * 교수님 정보 조회 (전체 공개)
     * 누구나 볼 수 있어야 하므로 필수 점수 0점 설정
     */
    @GetMapping("/about/professor")
    @ZeroTrust(requiredScore = 0)
    public String viewProfessor(Model model) {
        Professor prof = professorRepository.findAll().stream()
                .findFirst()
                .orElse(new Professor());
        model.addAttribute("prof", prof);
        return "about/professor";
    }

    /**
     * 교수님 정보 수정 (관리자 전용)
     * 교수님의 공신력 있는 데이터를 다루므로 90점 이상의 보안 점수를 요구합니다.
     */
    @PostMapping("/admin/professor/edit")
    @ResponseBody
    @ZeroTrust(requiredScore = 90) // 제로 트러스트 검증 추가
    public String editProfessor(@RequestParam String field, @RequestParam String content) {

        // [LOG] 수정 시도 기록
        log.info("[ZTA-PROF] 교수님 정보 수정 요청 감지. Field: {} (Verified Access Required)", field);

        Professor prof = professorRepository.findAll().stream()
                .findFirst()
                .orElse(new Professor());

        // 필드별 데이터 업데이트
        switch (field) {
            case "basic" -> prof.setBasicInfo(content);
            case "research" -> prof.setResearchInterests(content);
            case "edu" -> prof.setEducation(content);
            case "awards" -> prof.setAwards(content);
            default -> {
                log.warn("[ZTA-PROF] 유효하지 않은 필드 수정 시도: {}", field);
                return "invalid field";
            }
        }

        professorRepository.save(prof);

        // [LOG] 최종 성공 기록
        log.info("[ZTA-PROF] 교수님 '{}' 정보 업데이트 완료. (Policy Enforcement Success)", field);

        return "success";
    }
}