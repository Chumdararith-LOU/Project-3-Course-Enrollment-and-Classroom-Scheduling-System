package com.cource.controller;

import com.cource.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("permitAll()")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping({ "/", "/index" })
    public String index() {
        return "index";
    }

    @GetMapping("/signin")
    public String signinPage() {
        return "auth/signin";
    }

    @PostMapping("/api/auth/signin")
    public String signin(@RequestParam String email, @RequestParam String password, HttpServletResponse response) {
        log.info("Login attempt for email: {}", email);
        try {
            var result = authService.signIn(email, password);
            log.info("Login successful for email: {}, role: {}", email, result.getRoleCode());

            Cookie cookie = new Cookie("JWT", result.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            response.addCookie(cookie);

            log.info("Redirecting to: {}", result.getRedirectUrl());
            return "redirect:" + result.getRedirectUrl();
        } catch (IllegalArgumentException ex) {
            log.warn("Login failed: {}", ex.getMessage());
            return "redirect:/signin?error";
        }
    }

    @GetMapping("/signout")
    public String signout(HttpServletResponse response) {
        Cookie cookie = new Cookie("JWT", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";
    }

}
