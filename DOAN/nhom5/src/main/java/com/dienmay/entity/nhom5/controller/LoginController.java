package com.dienmay.entity.nhom5.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "pages/login";
    }

    @PostMapping("/login")
    public String loginPost() {
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logoutGet(HttpServletRequest request) {
        return logout(request);
    }

    @PostMapping("/logout")
    public String logoutPost(HttpServletRequest request) {
        return logout(request);
    }

    private String logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}