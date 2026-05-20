package com.seal.seal_lab.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessDeniedPageController {

    @GetMapping("/access-denied")
    public String accessDeniedPage(HttpServletRequest request, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String fallbackUserName = (authentication != null) ? authentication.getName() : "anonymousUser";

        model.addAttribute("denialType", request.getAttribute("denialType") != null
                ? request.getAttribute("denialType")
                : "AUTH");
        model.addAttribute("userName", request.getAttribute("userName") != null
                ? request.getAttribute("userName")
                : fallbackUserName);
        model.addAttribute("errorMessage", request.getAttribute("errorMessage"));
        model.addAttribute("trustScore", request.getAttribute("trustScore"));
        model.addAttribute("requiredScore", request.getAttribute("requiredScore"));
        model.addAttribute("showStepUpButton", request.getAttribute("showStepUpButton") != null
                ? request.getAttribute("showStepUpButton")
                : false);
        model.addAttribute("stepUpTargetUri", request.getAttribute("stepUpTargetUri"));
        model.addAttribute("stepUpReturnRequiresRetry", request.getAttribute("stepUpReturnRequiresRetry"));

        return "error/access-denied";
    }
}
