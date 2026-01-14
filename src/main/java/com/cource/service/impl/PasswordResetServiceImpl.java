package com.cource.service.impl;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cource.entity.PasswordResetToken;
import com.cource.repository.PasswordResetTokenRepository;
import com.cource.repository.UserRepository;
import com.cource.service.PasswordResetService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String createResetToken(String email) {
        var user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found with this email.");
        }

        tokenRepository.findByUser(user).ifPresent(tokenRepository::delete);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        // Print reset instructions to console for testing
        String resetUrl = "http://localhost:8080/api/auth/reset-password";
        System.out.println("\n\n✅ FORGOT PASSWORD - Use this token to reset:");
        System.out.println("Token: " + token);
        System.out.println("POST to: " + resetUrl);
        System.out.println("Body: {\"token\": \"" + token + "\", \"password\": \"yourNewPassword\"}");
        System.out.println("\n");

        return token;
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        var prt = tokenRepository.findByToken(token).orElse(null);
        if (prt == null) {
            throw new IllegalArgumentException("Invalid or expired token.");
        }

        if (prt.isExpired()) {
            tokenRepository.delete(prt);
            throw new IllegalArgumentException("Token expired.");
        }

        var user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(prt);
    }
}
