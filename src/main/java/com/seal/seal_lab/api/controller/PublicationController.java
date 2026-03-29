package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.Publication;
import com.seal.seal_lab.core.enums.PubCategory;
import com.seal.seal_lab.infra.repository.PublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PublicationController {
    private final PublicationRepository pubRepository;

//    @GetMapping("/publications")
//    public String list(Model model) {
//        // 모든 데이터를 가져와서 카테고리별로 그룹화 (날짜 내림차순 정렬 포함)
//        Map<PubCategory, List<Publication>> groupedPubs = pubRepository.findAll(Sort.by(Sort.Direction.DESC, "publishDate"))
//                .stream()
//                .collect(Collectors.groupingBy(Publication::getCategory));
//
//        model.addAttribute("groupedPubs", groupedPubs);
//        model.addAttribute("categories", PubCategory.values()); // 루프용
//        return "publications/list";
//    }
    @GetMapping("/publications")
    public String list(Model model) {
        List<Publication> allPubs = pubRepository.findAll(Sort.by(Sort.Direction.DESC, "publishDate"));

        // 디버깅 로그 (유지)
        System.out.println(">>> DB에서 가져온 논문 총 개수: " + allPubs.size());

        // 1. 키를 String으로 사용하는 LinkedHashMap 생성
        Map<String, List<Publication>> groupedPubs = new LinkedHashMap<>();

        // 2. 모든 카테고리에 대해 빈 리스트 미리 생성
        for (PubCategory cat : PubCategory.values()) {
            groupedPubs.put(cat.name(), new ArrayList<>());
        }

        // 3. 데이터를 카테고리 이름(String)을 키로 하여 분류
        for (Publication pub : allPubs) {
            if (pub.getCategory() != null) {
                String key = pub.getCategory().name(); // 예: "INT_JOURNAL"
                groupedPubs.get(key).add(pub);
            }
        }

        model.addAttribute("groupedPubs", groupedPubs);
        model.addAttribute("categories", PubCategory.values());
        return "publications/list";
    }

    @PostMapping("/admin/publications/add")
    public String add(@ModelAttribute Publication pub) {
        pubRepository.save(pub);
        return "redirect:/publications";
    }

    @PostMapping("/admin/publications/delete/{id}")
    public String delete(@PathVariable Long id) {
        pubRepository.deleteById(id);
        return "redirect:/publications";
    }
    // 1. 수정 폼으로 이동
    @GetMapping("/admin/publications/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Publication pub = pubRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid pub Id:" + id));

        model.addAttribute("pub", pub);
        model.addAttribute("categories", PubCategory.values()); // 카테고리 선택용
        return "publications/edit"; // 편집 페이지
    }

    // 2. 수정 실행
    @PostMapping("/admin/publications/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Publication pub) {
        Publication existingPub = pubRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid pub Id:" + id));

        // 데이터 업데이트
        existingPub.setCategory(pub.getCategory());
        existingPub.setTitle(pub.getTitle());
        existingPub.setAuthors(pub.getAuthors());
        existingPub.setVenue(pub.getVenue());
        existingPub.setPublishDate(pub.getPublishDate());
        existingPub.setLink(pub.getLink());

        pubRepository.save(existingPub);
        return "redirect:/publications";
    }
}
