package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.Professor;
import com.seal.seal_lab.infra.repository.ProfessorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
public class ProfessorController {
    private final ProfessorRepository professorRepository;

    @GetMapping("/about/professor")
    public String viewProfessor(Model model) {
        Professor prof = professorRepository.findAll().stream().findFirst().orElse(new Professor());
        model.addAttribute("prof", prof);
        return "about/professor";
    }

    @PostMapping("/admin/professor/edit")
    @ResponseBody
    public String editProfessor(@RequestParam String field, @RequestParam String content) {
        Professor prof = professorRepository.findAll().stream().findFirst().orElse(new Professor());
        if ("basic".equals(field)) prof.setBasicInfo(content);
        else if ("research".equals(field)) prof.setResearchInterests(content);
        else if ("edu".equals(field)) prof.setEducation(content);
        else if ("awards".equals(field)) prof.setAwards(content);

        professorRepository.save(prof);
        return "success";
    }
}
