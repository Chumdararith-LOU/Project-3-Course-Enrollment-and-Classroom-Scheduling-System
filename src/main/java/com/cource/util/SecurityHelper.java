package com.cource.util;

import org.springframework.security.core.context.SecurityContextHolder;

import com.cource.repository.UserRepository;
import org.springframework.security.core.Authentication;

public class SecurityHelper {
    private final UserRepository userRepository;

    public SecurityHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null)
            return null;
        var user = userRepository.findByEmail(auth.getName());
        return user.map(u -> u.getId()).orElse(null);
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
