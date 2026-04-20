package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust; // 어노테이션 임포트
import com.seal.seal_lab.core.entity.Publication;
import com.seal.seal_lab.core.enums.PubCategory;
import com.seal.seal_lab.infra.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j // 보안 감사 로그를 위해 추가
public class PublicationController {

    private final PublicationRepository pubRepository;

    /**
     * 논문 목록 조회 (전체 공개)
     * 방문자들도 실적을 확인해야 하므로 문턱을 없앱니다.
     */
    @GetMapping("/publications")
    @ZeroTrust(requiredScore = 0)
    public String list(Model model) {
        List<Publication> allPubs = pubRepository.findAll(Sort.by(Sort.Direction.DESC, "publishDate"));

        Map<String, List<Publication>> groupedPubs = new LinkedHashMap<>();
        for (PubCategory cat : PubCategory.values()) {
            groupedPubs.put(cat.name(), new ArrayList<>());
        }

        for (Publication pub : allPubs) {
            if (pub.getCategory() != null) {
                groupedPubs.get(pub.getCategory().name()).add(pub);
            }
        }

        model.addAttribute("groupedPubs", groupedPubs);
        model.addAttribute("categories", PubCategory.values());
        return "publications/list";
    }

    /**
     * 논문 추가 처리
     * 실적 데이터를 생성하는 작업이므로 신뢰 상태(90점)를 요구합니다.
     */
    @PostMapping("/admin/publications/add")
    @ZeroTrust(requiredScore = 90)
    public String add(@ModelAttribute Publication pub) {
        pubRepository.save(pub);
        log.info("[ZTA-PUB] 새 논문 등록 완료: '{}' (Verified)", pub.getTitle());
        return "redirect:/publications";
    }

    /**
     * 논문 수정 폼 이동
     * 관리자 기능을 노출하는 것부터 점수 체크를 진행합니다.
     */
    @GetMapping("/admin/publications/edit/{id}")
    @ZeroTrust(requiredScore = 90)
    public String editForm(@PathVariable Long id, Model model) { // @PathVariable 최적화
        Publication pub = pubRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid pub Id:" + id));

        model.addAttribute("pub", pub);
        model.addAttribute("categories", PubCategory.values());
        return "publications/edit";
    }

    /**
     * 논문 수정 실행
     */
    @PostMapping("/admin/publications/edit/{id}")
    @ZeroTrust(requiredScore = 90)
    public String update(@PathVariable Long id, @ModelAttribute Publication pub) {
        Publication existingPub = pubRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid pub Id:" + id));

        existingPub.setCategory(pub.getCategory());
        existingPub.setTitle(pub.getTitle());
        existingPub.setAuthors(pub.getAuthors());
        existingPub.setVenue(pub.getVenue());
        existingPub.setPublishDate(pub.getPublishDate());
        existingPub.setLink(pub.getLink());

        pubRepository.save(existingPub);
        log.info("[ZTA-PUB] 논문 데이터 수정 완료: ID {}", id);
        return "redirect:/publications";
    }

    /**
     * 논문 삭제 처리
     * 연구실 실적 파괴 행위는 가장 엄격하게(95점) 통제합니다.
     */
    @PostMapping("/admin/publications/delete/{id}")
    @ZeroTrust(requiredScore = 95)
    public String delete(@PathVariable Long id) {
        pubRepository.deleteById(id);
        log.warn("[ZTA-PUB] 논문 삭제 발생! 대상 ID: {} (High Trust Required)", id);
        return "redirect:/publications";
    }
}