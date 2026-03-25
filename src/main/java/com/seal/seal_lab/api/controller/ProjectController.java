package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.Project;
import com.seal.seal_lab.infra.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectRepository projectRepository;

    @GetMapping
    public String listProjects(Model model) {
        model.addAttribute("projects", projectRepository.findAllByOrderByIdDesc());
        return "projects/list";
    }

    // 과제 추가 (관리자 전용)
    @PostMapping("/admin/add")
    public String addProject(@ModelAttribute Project project) {
        projectRepository.save(project);
        return "redirect:/projects";
    }

    // 과제 삭제 (관리자 전용)
    @PostMapping("/admin/delete/{id}")
    public String deleteProject(@PathVariable Long id) {
        projectRepository.deleteById(id);
        return "redirect:/projects";
    }

    // 과제 수정 (관리자 전용)
    @PostMapping("/admin/update")
    public String updateProject(@ModelAttribute Project project) {
        projectRepository.save(project); // ID가 포함되어 있으면 수정으로 작동합니다.
        return "redirect:/projects";
    }
}
