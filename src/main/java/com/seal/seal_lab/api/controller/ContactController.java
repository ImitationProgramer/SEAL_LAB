package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.entity.ContactInfo;
import com.seal.seal_lab.infra.repository.ContactInfoRepository;
import lombok.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ContactController {
    private final ContactInfoRepository contactRepository;

    @GetMapping("/contact")
    public String viewContact(Model model) {
        // 1. 우선 첫 번째 데이터를 찾습니다. (ID에 의존하기보다 전체 중 첫 번째를 가져오는 게 안전합니다)
        ContactInfo contact = contactRepository.findAll().stream().findFirst().orElse(null);

        // 2. 만약 데이터가 하나도 없다면 새로 만듭니다.
        if (contact == null) {
            contact = contactRepository.save(ContactInfo.builder()
                    // .id(1L) <- 이 부분을 지워야 합니다! DB가 자동으로 번호를 매기게 하세요.
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

    @PostMapping("/admin/contact/update")
    public String updateContact(@ModelAttribute ContactInfo contact) {
        // 업데이트 시에는 기존의 ID를 유지해야 합니다.
        // 만약 첫 번째 데이터의 ID가 1이 아닐 수도 있으니, 실제 있는 ID를 세팅해주는 게 좋습니다.
        ContactInfo existing = contactRepository.findAll().stream().findFirst().orElse(null);
        if (existing != null) {
            contact.setId(existing.getId());
        }
        contactRepository.save(contact);
        return "redirect:/contact";
    }
}
