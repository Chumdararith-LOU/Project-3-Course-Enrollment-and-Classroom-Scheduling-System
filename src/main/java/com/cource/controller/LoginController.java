package com.cource.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

//    @GetMapping("/")
//    public String home() {
//        return "redirect:/login";
//    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/403";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/";
    }
}