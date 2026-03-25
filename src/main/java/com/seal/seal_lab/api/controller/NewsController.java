package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.News;
import com.seal.seal_lab.infra.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class NewsController {

    private final NewsRepository newsRepository;

    // 1. 뉴스 작성 폼 이동 (관리자 전용)
    @GetMapping("/admin/news/add")
    public String addNewsForm() {
        return "news/add";
    }

    // 2. 뉴스 저장 로직
    @PostMapping("/admin/news/add")
    public String saveNews(@RequestParam("title") String title,
                           @RequestParam("content") String content) {
        News news = News.builder()
                .title(title)
                .content(content)
                .build();
        newsRepository.save(news);
        return "redirect:/"; // 저장 후 메인 페이지로 이동
    }

    // NewsController.java 에 추가
    @PostMapping("/admin/news/delete/{id}")
    public String deleteNews(@PathVariable("id") Long id) {
        newsRepository.deleteById(id);
        return "redirect:/";
    }
}