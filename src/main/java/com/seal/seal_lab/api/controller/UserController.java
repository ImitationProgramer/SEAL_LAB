package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.api.dto.UserSignupDto;
import com.seal.seal_lab.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 1. 회원가입 페이지 이동
     */
    @GetMapping("/signup")
    public String signupForm(Model model) {
        // 타임리프 폼 바인딩을 위해 빈 DTO 객체를 모델에 담아 보냅니다.
        model.addAttribute("userSignupDto", new UserSignupDto());
        return "auth/signup";
    }

    /**
     * 2. 회원가입 처리
     */
    @PostMapping("/signup")
    public String register(@Valid @ModelAttribute("userSignupDto") UserSignupDto dto,
                           BindingResult result,
                           Model model) {

        // [검증 1] DTO에 설정한 유효성 검사(Size, Pattern 등) 결과 확인
        if (result.hasErrors()) {
            log.warn("회원가입 입력값 검증 실패: {}", result.getAllErrors());
            return "auth/signup"; // 에러 메시지와 함께 가입 페이지로 유지
        }

        try {
            // [비즈니스 로직] 서비스 호출 (중복 체크, 암호화, 저장)
            userService.register(dto);
            log.info("회원가입 성공: ID={}", dto.getLoginId());
            return "redirect:/login?signupSuccess=true"; // 가입 성공 시 로그인 페이지로 이동

        } catch (IllegalStateException e) {
            // [검증 2] 중복 아이디 등 서비스 계층에서 발생한 예외 처리
            log.warn("회원가입 비즈니스 로직 에러: {}", e.getMessage());
            result.rejectValue("loginId", "duplicate", e.getMessage());
            return "auth/signup";
        }
    }
}