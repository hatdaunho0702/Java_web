package com.dienmay.entity.nhom5.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegisterController {

    @GetMapping("/register")
    public String register() {
        return "pages/register";
    }

    @PostMapping("/register")
    public String registerPost() {
        return "redirect:/register";
    }
}