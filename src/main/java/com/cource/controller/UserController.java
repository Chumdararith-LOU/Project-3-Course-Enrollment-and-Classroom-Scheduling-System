package com.cource.controller;

import com.cource.dto.user.UserProfileDTO;
import com.cource.entity.User;
import com.cource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/profile")
    public ResponseEntity<String> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute UserProfileDTO dto,
            @RequestParam(required = false) MultipartFile avatar) {

        User user = userService.getUserByEmail(userDetails.getUsername());
        userService.updateProfile(user.getId(), dto, avatar);
        return ResponseEntity.ok("Profile updated");
    }
}
