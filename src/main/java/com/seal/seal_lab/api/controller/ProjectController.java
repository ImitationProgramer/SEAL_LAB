package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust; // 어노테이션 임포트
import com.seal.seal_lab.core.entity.Project;
import com.seal.seal_lab.infra.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects")
@Slf4j // 로그 기록을 위해 추가
public class ProjectController {

    private final ProjectRepository projectRepository;

    /**
     * 과제 목록 조회
     * 외부 방문자나 신규 멤버도 봐야 하므로 문턱을 없앱니다.
     */
    @GetMapping
    @ZeroTrust(requiredScore = 0)
    public String listProjects(Model model) {
        model.addAttribute("projects", projectRepository.findAllByOrderByIdDesc());
        return "projects/list";
    }

    /**
     * 과제 추가 (관리자 전용)
     * 새로운 연구 데이터를 생성하는 작업이므로 신뢰 상태(90점)를 요구합니다.
     */
    @PostMapping("/admin/add")
    @ZeroTrust(requiredScore = 90)
    public String addProject(@ModelAttribute Project project) {
        projectRepository.save(project);
        log.info("[ZTA-PROJECT] 새 과제 등록 성공: '{}' (User Score Verified)", project.getTitle());
        return "redirect:/projects";
    }

    /**
     * 과제 수정 (관리자 전용)
     * 기존 데이터를 변경하는 작업이므로 '추가'와 동일한 90점을 요구합니다.
     */
    @PostMapping("/admin/update")
    @ZeroTrust(requiredScore = 90)
    public String updateProject(@ModelAttribute Project project) {
        projectRepository.save(project); // ID가 포함되어 있으면 수정으로 작동
        log.info("[ZTA-PROJECT] 과제 정보 수정 완료: ID {}", project.getId());
        return "redirect:/projects";
    }

    /**
     * 과제 삭제 (관리자 전용)
     * 데이터 유실 위험이 있는 가장 민감한 작업입니다.
     * 최고 수준의 신뢰도(95점)가 필요합니다.
     */
    @PostMapping("/admin/delete/{id}")
    @ZeroTrust(requiredScore = 95)
    public String deleteProject(@PathVariable Long id) {
        projectRepository.deleteById(id);
        log.warn("[ZTA-PROJECT] 과제 삭제 감지! 대상 ID: {} (Critical Action Verified)", id);
        return "redirect:/projects";
    }
}