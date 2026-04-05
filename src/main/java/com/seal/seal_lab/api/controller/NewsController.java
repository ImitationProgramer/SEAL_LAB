package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust; // 어노테이션 임포트
import com.seal.seal_lab.core.entity.News;
import com.seal.seal_lab.infra.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그 기록을 위해 추가
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsRepository newsRepository;

    /**
     * 뉴스 작성 폼 이동 (관리자 전용)
     * 폼에 접근하는 것부터 신뢰 상태(90점)를 요구하여 불필요한 노출을 차단합니다.
     */
    @GetMapping("/admin/news/add")
    @ZeroTrust(requiredScore = 90)
    public String addNewsForm() {
        log.info("[ZTA-NEWS] 뉴스 작성 페이지 접근 시도됨.");
        return "news/add";
    }

    /**
     * 뉴스 저장 로직
     * 연구실 소식을 공표하는 작업이므로 90점 이상의 보안 상태가 필요합니다.
     */
    @PostMapping("/admin/news/add")
    @ZeroTrust(requiredScore = 90)
    public String saveNews(@RequestParam("title") String title,
                           @RequestParam("content") String content) {

        News news = News.builder()
                .title(title)
                .content(content)
                .build();

        newsRepository.save(news);
        log.info("[ZTA-NEWS] 새 소식 등록 완료: '{}' (ZTA Verified)", title);

        return "redirect:/"; // 저장 후 메인 페이지로 이동
    }

    /**
     * 뉴스 삭제 (관리자 전용)
     * 기록된 소식을 삭제하는 것은 민감한 작업이므로 최고 점수(95점)를 요구합니다.
     */
    @PostMapping("/admin/news/delete/{id}")
    @ZeroTrust(requiredScore = 95)
    public String deleteNews(@PathVariable Long id) {
        newsRepository.deleteById(id);
        log.warn("[ZTA-NEWS] 뉴스 데이터 삭제 발생! 대상 ID: {} (Critical Path Cleared)", id);

        return "redirect:/";
    }
}