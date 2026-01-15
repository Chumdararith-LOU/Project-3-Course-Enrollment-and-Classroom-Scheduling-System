package com.cource.service.impl;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

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

        // Log token creation (do not log the actual token in production!)
        log.info("Password reset token created for user: {}", email);

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
