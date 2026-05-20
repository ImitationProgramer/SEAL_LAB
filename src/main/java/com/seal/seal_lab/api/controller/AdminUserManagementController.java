package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.api.service.AdminUserManagementService;
import com.seal.seal_lab.core.annotation.ZeroTrust;
import com.seal.seal_lab.core.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;

    @GetMapping("/admin/users")
    @ZeroTrust(requiredScore = 90)
    public String userManagement(@RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "role", required = false) String role,
                                 @RequestParam(value = "labRank", required = false) String labRank,
                                 Model model) {
        try {
            List<User> users = adminUserManagementService.searchUsers(keyword, role, labRank);
            model.addAttribute("users", users);
        } catch (IllegalArgumentException e) {
            model.addAttribute("users", List.of());
            model.addAttribute("userFilterError", e.getMessage());
        }

        model.addAttribute("keywordFilter", keyword);
        model.addAttribute("roleFilter", role);
        model.addAttribute("labRankFilter", labRank);
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("labRanks", User.LabRank.values());
        return "admin/users";
    }

    @PostMapping("/admin/users/{loginId}/lab-rank")
    @ZeroTrust(requiredScore = 95)
    public String updateLabRank(@PathVariable("loginId") String loginId,
                                @RequestParam("labRank") String labRank,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "role", required = false) String role,
                                @RequestParam(value = "currentLabRank", required = false) String currentLabRank,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            adminUserManagementService.updateLabRank(loginId, labRank, authentication.getName());
            redirectAttributes.addFlashAttribute("userOperationMessage",
                    "'" + loginId + "' 계정의 연구실 직급을 변경했습니다.");
        } catch (IllegalArgumentException | NoSuchElementException e) {
            redirectAttributes.addFlashAttribute("userOperationError", e.getMessage());
        }

        return buildRedirectUrl(keyword, role, currentLabRank);
    }

    private String buildRedirectUrl(String keyword, String role, String currentLabRank) {
        StringBuilder redirect = new StringBuilder("redirect:/admin/users");
        boolean hasQuery = false;

        hasQuery = appendQuery(redirect, "keyword", keyword, hasQuery);
        hasQuery = appendQuery(redirect, "role", role, hasQuery);
        appendQuery(redirect, "labRank", currentLabRank, hasQuery);
        return redirect.toString();
    }

    private boolean appendQuery(StringBuilder redirect, String key, String value, boolean hasQuery) {
        if (value == null || value.isBlank()) {
            return hasQuery;
        }
        redirect.append(hasQuery ? "&" : "?")
                .append(key)
                .append("=")
                .append(value);
        return true;
    }
}
