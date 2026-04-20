package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust; // 어노테이션 임포트
import com.seal.seal_lab.core.entity.ContactInfo;
import com.seal.seal_lab.infra.repository.ContactInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 보안 로그 기록을 위해 추가
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final ContactInfoRepository contactRepository;

    /**
     * 연락처 및 위치 정보 조회 (전체 공개)
     * 방문객이 위치를 찾아와야 하므로 문턱을 0점으로 설정합니다.
     */
    @GetMapping("/contact")
    @ZeroTrust(requiredScore = 0)
    public String viewContact(Model model) {
        // 첫 번째 데이터 조회 (데이터가 없으면 기본값으로 생성)
        ContactInfo contact = contactRepository.findAll().stream().findFirst().orElse(null);

        if (contact == null) {
            log.info("[ZTA-INFO] 초기 연락처 정보가 없어 기본 데이터를 생성합니다.");
            contact = contactRepository.save(ContactInfo.builder()
                    .addressKr("경기도 용인시 기흥구 강남로 40 강남대학교 이공관")
                    .addressEn("Robot Convergence Building, 40 Gangnam-ro, Giheung-gu, Yongin-si, Gyeonggi-do, 16979, Republic of Korea")
                    .phone("031-280-3694")
                    .labHeadKr("이여민")
                    .email("1252037@kangnam.ac.kr")
                    .latitude(37.27703)
                    .longitude(127.13422)
                    .build());
        }

        model.addAttribute("contact", contact);
        return "contact/view";
    }

    /**
     * 연락처 정보 업데이트 (관리자 전용)
     * 연구실의 공식 정보를 변경하는 민감한 작업이므로 90점 이상의 보안 점수를 요구합니다.
     */
    @PostMapping("/admin/contact/update")
    @ZeroTrust(requiredScore = 90)
    public String updateContact(@ModelAttribute ContactInfo contact) {

        // [LOG] 수정 시도 기록
        log.info("[ZTA-AUDIT] 연구실 연락처 정보 수정 요청됨. (Score Verification Required)");

        // 기존 ID 유지 로직
        ContactInfo existing = contactRepository.findAll().stream().findFirst().orElse(null);
        if (existing != null) {
            contact.setId(existing.getId());
        }

        contactRepository.save(contact);

        // [LOG] 최종 성공 기록
        log.info("[ZTA-SUCCESS] 연구실 연락처 정보 업데이트 완료. (User: Verified Admin)");

        return "redirect:/contact";
    }
}