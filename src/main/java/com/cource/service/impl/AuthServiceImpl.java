package com.cource.service.impl;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cource.config.JwtService;
import com.cource.dto.auth.AuthResult;
import com.cource.entity.Role;
import com.cource.entity.User;
import com.cource.repository.RoleRepository;
import com.cource.repository.UserRepository;
import com.cource.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResult signIn(String email, String password) {
        var user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String roleCode = user.getRole() != null ? user.getRole().getRoleCode() : null;
        String token = jwtService.generateToken(user.getEmail(), roleCode == null ? List.of() : List.of(roleCode));

        String redirectUrl;
        if ("ADMIN".equalsIgnoreCase(roleCode)) {
            redirectUrl = "/admin/dashboard?adminId=" + user.getId();
        } else if ("LECTURER".equalsIgnoreCase(roleCode)) {
            redirectUrl = "/lecturer/dashboard?lecturerId=" + user.getId();
        } else if ("STUDENT".equalsIgnoreCase(roleCode)) {
            redirectUrl = "/student/dashboard?studentId=" + user.getId();
        } else {
            redirectUrl = "/";
        }

        return new AuthResult(user.getId(), roleCode, token, redirectUrl);
    }

    @Override
    public AuthResult signUpStudent(String firstName, String lastName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role studentRole = roleRepository.findByRoleCode("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(studentRole);
        userRepository.save(user);

        String roleCode = user.getRole() != null ? user.getRole().getRoleCode() : null;
        String token = jwtService.generateToken(user.getEmail(), roleCode == null ? List.of() : List.of(roleCode));
        String redirectUrl = "/student/dashboard?studentId=" + user.getId();

        return new AuthResult(user.getId(), roleCode, token, redirectUrl);
    }
}
