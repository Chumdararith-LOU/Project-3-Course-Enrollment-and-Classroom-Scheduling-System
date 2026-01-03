package com.cource.controller;

import com.cource.dto.forgetpassword.ForgotPasswordRequest;
import com.cource.dto.forgetpassword.ResetPasswordRequest;
import com.cource.entity.PasswordResetToken;
import com.cource.entity.User;
import com.cource.repository.PasswordResetTokenRepository;
import com.cource.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found with this email.");
        }

        // Delete any existing token for this user
        tokenRepository.findByUser(userOpt.get()).ifPresent(tokenRepository::delete);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, userOpt.get());
        tokenRepository.save(resetToken);

        // 🔥 Print reset link in console (no email needed for testing)
        String resetUrl = "http://localhost:8080/api/auth/reset-password";
        System.out.println("\n\n✅ FORGOT PASSWORD - Use this token to reset:");
        System.out.println("Token: " + token);
        System.out.println("POST to: " + resetUrl);
        System.out.println("Body: {\"token\": \"" + token + "\", \"password\": \"yourNewPassword\"}");
        System.out.println("\n");

        return ResponseEntity.ok("Password reset instructions generated (check console).");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(request.getToken());
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.isExpired()) {
            tokenRepository.delete(token);
            return ResponseEntity.badRequest().body("Token expired.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        tokenRepository.delete(token);
        return ResponseEntity.ok("Password reset successfully.");
    }
}