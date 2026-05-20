package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.api.dto.UserProfileUpdateDto;
import com.seal.seal_lab.api.service.UserProfileService;
import com.seal.seal_lab.core.annotation.ZeroTrust;
import com.seal.seal_lab.core.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile/me/edit")
    @ZeroTrust(requiredScore = 70)
    public String myProfileEditForm(Authentication authentication, Model model) {
        User user = userProfileService.getUserProfile(authentication.getName());
        model.addAttribute("profileTarget", user);
        model.addAttribute("userProfileUpdateDto", userProfileService.toUpdateDto(user));
        model.addAttribute("editingSelf", true);
        model.addAttribute("formAction", "/profile/me/edit");
        return "profile/edit";
    }

    @PostMapping("/profile/me/edit")
    @ZeroTrust(requiredScore = 70)
    public String updateMyProfile(@Valid @ModelAttribute("userProfileUpdateDto") UserProfileUpdateDto dto,
                                  BindingResult result,
                                  MultipartFile file,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) throws IOException {
        User user = userProfileService.getUserProfile(authentication.getName());
        if (result.hasErrors()) {
            model.addAttribute("profileTarget", user);
            model.addAttribute("editingSelf", true);
            model.addAttribute("formAction", "/profile/me/edit");
            return "profile/edit";
        }

        userProfileService.updateProfile(user.getLoginId(), dto, file);
        redirectAttributes.addFlashAttribute("profileMessage", "내 프로필을 업데이트했습니다.");
        return "redirect:/profile/me/edit";
    }

    @GetMapping("/admin/users/{loginId}/edit")
    @ZeroTrust(requiredScore = 90)
    public String adminProfileEditForm(@PathVariable("loginId") String loginId, Model model) {
        User user = userProfileService.getUserProfile(loginId);
        model.addAttribute("profileTarget", user);
        model.addAttribute("userProfileUpdateDto", userProfileService.toUpdateDto(user));
        model.addAttribute("editingSelf", false);
        model.addAttribute("formAction", "/admin/users/" + user.getLoginId() + "/edit");
        return "profile/edit";
    }

    @PostMapping("/admin/users/{loginId}/edit")
    @ZeroTrust(requiredScore = 90)
    public String adminUpdateProfile(@PathVariable("loginId") String loginId,
                                     @Valid @ModelAttribute("userProfileUpdateDto") UserProfileUpdateDto dto,
                                     BindingResult result,
                                     MultipartFile file,
                                     Model model,
                                     RedirectAttributes redirectAttributes) throws IOException {
        User user = userProfileService.getUserProfile(loginId);
        if (result.hasErrors()) {
            model.addAttribute("profileTarget", user);
            model.addAttribute("editingSelf", false);
            model.addAttribute("formAction", "/admin/users/" + user.getLoginId() + "/edit");
            return "profile/edit";
        }

        userProfileService.updateProfile(user.getLoginId(), dto, file);
        redirectAttributes.addFlashAttribute("userOperationMessage",
                "'" + user.getLoginId() + "' 공개 프로필을 업데이트했습니다.");
        return "redirect:/admin/users";
    }
}
