package com.seal.seal_lab.infra.exception;

import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final UserRepository userRepository;
    private final HttpServletRequest request; // Aspect에서 담은 데이터를 꺼내기 위해 주입

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException e, Model model) {

        // 1. 사용자 정보 식별
        String loginId = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByLoginId(loginId).orElse(null);
        String displayName = (user != null) ? user.getName() : loginId;

        // 2. Aspect에서 넘겨준 Zero Trust 데이터 추출 (4090 버그 원천 차단)
        Object scoreAttr = request.getAttribute("ztaCurrentScore");
        Object requiredAttr = request.getAttribute("ztaRequiredScore");

        // 만약 ZTA에 의한 차단이 아닐 경우(일반 권한 부족 등)를 대비해 기본값 설정
        String currentScore = (scoreAttr != null) ? scoreAttr.toString() : "N/A";
        String requiredScore = (requiredAttr != null) ? requiredAttr.toString() : "0";

        // 3. 뷰(HTML)에 전달할 데이터 담기
        model.addAttribute("userName", displayName);
        model.addAttribute("trustScore", currentScore);      // 현재 유저 점수
        model.addAttribute("requiredScore", requiredScore);  // 해당 페이지 요구 점수
        model.addAttribute("errorMessage", e.getMessage());

        // 4. 보안 감사 로그 (구분자 '/'를 넣어 40/90 처럼 보이게 함)
        log.warn("[ZTA-Enforcement] User: {} | Score: {}/{} | Blocked at: {}",
                loginId, currentScore, requiredScore, request.getRequestURI());

        return "error/access-denied"; // 차단 전용 페이지로 이동
    }
}