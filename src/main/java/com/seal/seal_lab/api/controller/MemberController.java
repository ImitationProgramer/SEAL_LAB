package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust;
import com.seal.seal_lab.core.entity.User;
import com.seal.seal_lab.infra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final UserRepository userRepository;

    @GetMapping("/about/member")
    @ZeroTrust(requiredScore = 0)
    public String memberList(Model model) {
        List<User> visibleUsers = userRepository.findAll().stream()
                .filter(user -> user.getResolvedLabRank().isVisibleOnMemberPage())
                .sorted(Comparator
                        .comparingInt((User user) -> user.getResolvedLabRank().getDisplayOrder())
                        .thenComparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("members", visibleUsers);
        return "about/member";
    }
}
