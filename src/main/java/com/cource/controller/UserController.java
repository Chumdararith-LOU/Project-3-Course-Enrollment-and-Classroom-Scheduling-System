package com.cource.controller;

import com.cource.dto.user.UserProfileDTO;
import com.cource.dto.user.UserPublicProfileDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.User;
import com.cource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }
        User user = userService.getUserByEmail(userDetails.getUsername());

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

    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }
        User user = userService.getUserByEmail(userDetails.getUsername());

        UserProfileDTO profile = userService.getUserProfile(user.getId());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{id}/public-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserPublicProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }

        User user = userService.getUserById(id);

        UserProfileDTO profile = userService.getUserProfile(user.getId());
        UserPublicProfileDTO dto = new UserPublicProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getIdCard(),
                user.getRole() != null ? user.getRole().getRoleCode() : null,
                user.isActive(),
                profile);

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserUpdateRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }
        User user = userService.getUserByEmail(userDetails.getUsername());

        // Don't allow users to change their own role
        request.setRoleId(null);

        try {
            userService.updateUser(user.getId(), request);
            return ResponseEntity.ok(Collections.singletonMap("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping(path = "/me/avatar", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadCurrentUserAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Not authenticated"));
        }
        User user = userService.getUserByEmail(userDetails.getUsername());

        try {
            userService.updateAvatar(user.getId(), file);
            return ResponseEntity.ok(Collections.singletonMap("status", "success"));
        } catch (Exception e) {
            log.error("Avatar upload failed for user {}", user.getId(), e);
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = "Avatar upload failed: " + e.getClass().getSimpleName();
            }
            return ResponseEntity.status(500).body(Collections.singletonMap("error", errorMsg));
        }
    }

    @GetMapping("/me/avatar-image")
    public ResponseEntity<Resource> getCurrentUserAvatarImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long userId) {

        // If userId is provided, show that user's avatar (for viewing other profiles)
        if (userId != null) {
            return getUserAvatarById(userId);
        }

        // Otherwise, show current authenticated user's avatar
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByEmail(userDetails.getUsername());

        return getUserAvatarById(user.getId());
    }

    @GetMapping("/avatar/{userId}")
    public ResponseEntity<Resource> getUserAvatar(@PathVariable Long userId) {
        return getUserAvatarById(userId);
    }

    private ResponseEntity<Resource> getUserAvatarById(Long userId) {
        try {
            Resource res = userService.loadAvatarResource(userId);
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
