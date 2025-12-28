package com.cource.controller;

import com.cource.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login-api")
    public ResponseEntity<Map<String, Object>> loginApi(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletResponse response) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Authenticate using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Get role from authentication
            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(authority -> authority.getAuthority())
                    .orElse("ROLE_USER");
            
            // Generate JWT token
            String token = jwtTokenProvider.generateToken(email, role);
            
            // Set token in cookie
            setAuthCookie(response, token);
            
            // Determine redirect URL based on role
            String redirectUrl = determineRedirectUrl(role);
            
            result.put("success", true);
            result.put("token", token);
            result.put("redirect", redirectUrl);
            result.put("role", role);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Invalid email or password");
        }
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout-api")
    public ResponseEntity<Map<String, String>> logoutApi(HttpServletResponse response) {
        clearAuthCookie(response);
        SecurityContextHolder.clearContext();
        
        Map<String, String> result = new HashMap<>();
        result.put("message", "Logged out successfully");
        result.put("redirect", "/login");
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        String token = getTokenFromRequest(request);
        Map<String, Object> result = new HashMap<>();
        
        if (token != null && jwtTokenProvider.validateToken(token)) {
            result.put("valid", true);
            result.put("username", jwtTokenProvider.getUsernameFromToken(token));
            result.put("role", jwtTokenProvider.getRoleFromToken(token));
        } else {
            result.put("valid", false);
            result.put("message", "Invalid or expired token");
        }
        
        return ResponseEntity.ok(result);
    }

    private void setAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("AUTH-TOKEN", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 1 day
        response.addCookie(cookie);
    }

    private void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("AUTH-TOKEN", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        // Check Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // Check cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH-TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }

    private String determineRedirectUrl(String role) {
        switch (role) {
            case "ROLE_ADMIN":
                return "/admin/dashboard";
            case "ROLE_LECTURER":
                return "/lecturer/dashboard";
            case "ROLE_STUDENT":
                return "/student/dashboard";
            default:
                return "/";
        }
    }
}