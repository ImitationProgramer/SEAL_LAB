package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust;
import com.seal.seal_lab.core.entity.Member;
import com.seal.seal_lab.infra.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    // 멤버 목록 조회
    @GetMapping("/about/member")
    @ZeroTrust(requiredScore = 0)
    public String memberList(Model model) {
        model.addAttribute("members", memberRepository.findAll());
        return "about/member";
    }

    @PostMapping("/admin/member/add")
    @ZeroTrust(requiredScore = 90)
    public String addMember(@ModelAttribute Member member,
                            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {

        // 파일이 넘어왔고, 비어있지 않을 때만 저장 로직 실행
        if (file != null && !file.isEmpty()) {
            String projectPath = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

            // 폴더가 없으면 생성하는 코드 추가 (안전장치)
            File folder = new File(projectPath);
            if (!folder.exists()) folder.mkdirs();

            UUID uuid = UUID.randomUUID();
            String fileName = uuid + "_" + file.getOriginalFilename();
            File saveFile = new File(projectPath, fileName);
            file.transferTo(saveFile);

            member.setImagePath("/uploads/" + fileName);
        } else {
            // 파일을 선택하지 않았다면 DB의 imagePath는 null로 들어갑니다.
            member.setImagePath(null);
        }

        memberRepository.save(member);
        return "redirect:/about/member";
    }

    // 멤버 삭제
    @PostMapping("/admin/member/delete/{id}")
    public String deleteMember(@PathVariable Long id) {
        memberRepository.deleteById(id);
        return "redirect:/about/member";
    }
    // 1. 수정 페이지 이동
    @GetMapping("/admin/member/edit/{id}")
    public String editMemberForm(@PathVariable Long id, Model model) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid member Id:" + id));
        model.addAttribute("member", member);
        return "about/member_edit";
    }
    // 2. 수정 실행
    @PostMapping("/admin/member/edit/{id}")
    public String updateMember(@PathVariable Long id,
                               @ModelAttribute Member member,
                               @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {

        Member existingMember = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid member Id:" + id));

        // 기본 정보 업데이트
        existingMember.setName(member.getName());
        existingMember.setDegree(member.getDegree());
        existingMember.setRole(member.getRole());
        existingMember.setDepartment(member.getDepartment());
        existingMember.setEmail(member.getEmail());
        existingMember.setKeywords(member.getKeywords());

        // 새 사진이 업로드된 경우에만 교체
        if (file != null && !file.isEmpty()) {
            String projectPath = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            file.transferTo(new File(projectPath, fileName));
            existingMember.setImagePath("/uploads/" + fileName);
        }

        memberRepository.save(existingMember);
        return "redirect:/about/member";
    }
}