package com.cource.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        // For development convenience, redirect root to student dashboard
        return "redirect:/student/dashboard";
    }
}
