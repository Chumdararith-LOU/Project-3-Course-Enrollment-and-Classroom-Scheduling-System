package com.cource.controller;

import com.cource.entity.Role;
import com.cource.entity.User;
import com.cource.repository.RoleRepository;
import com.cource.repository.UserRepository;
import com.cource.config.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @GetMapping({ "/", "/index" })
    public String index() {
        return "index";
    }

    @GetMapping("/signin")
    public String signinPage() {
        return "auth/signin";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "auth/signup";
    }

    @PostMapping("/api/auth/signin")
    public String signin(@RequestParam String email, @RequestParam String password, HttpServletResponse response) {
        log.info("Login attempt for email: {}", email);
        var user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("Login failed: user not found for email: {}", email);
            return "redirect:/signin?error";
        }

        // manual password check to avoid AuthenticationManager configuration issues
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed: incorrect password for email: {}", email);
            return "redirect:/signin?error";
        }

        log.info("Login successful for email: {}, role: {}", email, user.getRole().getRoleCode());
        String token = jwtService.generateToken(user.getEmail(), List.of(user.getRole().getRoleCode()));
        Cookie cookie = new Cookie("JWT", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        String role = user.getRole().getRoleCode();
        String redirectUrl;
        if ("ADMIN".equalsIgnoreCase(role)) {
            redirectUrl = "/admin/dashboard?adminId=" + user.getId();
        } else if ("LECTURER".equalsIgnoreCase(role)) {
            redirectUrl = "/lecturer/dashboard?lecturerId=" + user.getId();
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            redirectUrl = "/student/dashboard?studentId=" + user.getId();
        } else {
            redirectUrl = "/";
        }
        log.info("Redirecting to: {}", redirectUrl);
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/api/auth/signup")
    public String signup(@RequestParam String firstName, @RequestParam String lastName,
            @RequestParam String email, @RequestParam String password, HttpServletResponse response) {
        if (userRepository.existsByEmail(email)) {
            return "redirect:/signup?error=exists";
        }

        Role studentRole = roleRepository.findByRoleCode("STUDENT")
                .orElseThrow(() -> new RuntimeException("STUDENT role not found"));
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(studentRole);
        userRepository.save(user);

        // auto login
        String token = jwtService.generateToken(user.getEmail(), List.of(user.getRole().getRoleCode()));
        Cookie cookie = new Cookie("JWT", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/student/dashboard?studentId=" + user.getId();
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
