package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.LabIntro;
import com.seal.seal_lab.infra.repository.LabIntroRepository;
import com.seal.seal_lab.infra.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor // final이 붙은 필드를 자동으로 생성자 주입해줍니다.
public class IndexController {
    private final NewsRepository newsRepository;
    private final LabIntroRepository labIntroRepository;

    @GetMapping("/")
    public String index(Model model) {
        // DB에서 뉴스 목록을 최신순(createdAt 내림차순)으로 가져옵니다.
        model.addAttribute("newsList", newsRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
// DB에 데이터가 하나만 있을 것이므로 첫 번째 데이터를 가져옵니다.
        List<LabIntro> intros = labIntroRepository.findAll();
        LabIntro labIntro = intros.isEmpty() ? null : intros.get(0);

        model.addAttribute("labIntro", labIntro);
        // templates/home.html을 보여줍니다.
        return "home";
    }
}