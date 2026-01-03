package com.cource.controller;

import com.cource.dto.user.UserProfileDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.User;
import com.cource.repository.UserRepository;
import com.cource.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Get the current authenticated user's basic info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", "User not found"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("idCard", user.getIdCard());
        response.put("role", user.getRole().getRoleCode());
        response.put("isActive", user.isActive());

        return ResponseEntity.ok(response);
    }

    /**
     * Get the current authenticated user's profile (extended info)
     */
    @GetMapping("/me/profile")
    public ResponseEntity<?> getCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", "User not found"));
        }

        UserProfileDTO profile = userService.getUserProfile(user.getId());
        return ResponseEntity.ok(profile);
    }

    /**
     * Update the current authenticated user's profile
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserUpdateRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", "User not found"));
        }

        // Don't allow users to change their own role
        request.setRoleId(null);

        try {
            userService.updateUser(user.getId(), request);
            return ResponseEntity.ok(Collections.singletonMap("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Upload avatar for the current authenticated user
     */
    @PostMapping(path = "/me/avatar", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadCurrentUserAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", "User not found"));
        }

        try {
            userService.updateAvatar(user.getId(), file);
            return ResponseEntity.ok(Collections.singletonMap("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Get avatar image for the current authenticated user
     */
    @GetMapping("/me/avatar-image")
    public ResponseEntity<Resource> getCurrentUserAvatarImage(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Resource res = userService.loadAvatarResource(user.getId());
            if (res == null) {
                // serve default avatar
                ClassPathResource def = new ClassPathResource("static/images/default-avatar.svg");
                if (!def.exists())
                    return ResponseEntity.notFound().build();
                return ResponseEntity.ok()
                        .header("Cache-Control", "max-age=3600")
                        .contentType(MediaType.parseMediaType("image/svg+xml"))
                        .body(def);
            }
            String contentType = null;
            try {
                contentType = Files.probeContentType(Paths.get(res.getURI()));
            } catch (Exception ex) {
                // ignore
            }
            if (contentType == null)
                contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .header("Cache-Control", "max-age=3600")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
