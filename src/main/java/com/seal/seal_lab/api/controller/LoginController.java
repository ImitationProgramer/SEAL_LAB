package com.seal.seal_lab.api.controller;

import com.seal.seal_lab.core.annotation.ZeroTrust;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    @ZeroTrust(requiredScore = 0)
    public String loginPage() {
        return "login"; // templates/login.html을 찾습니다.
    }
}