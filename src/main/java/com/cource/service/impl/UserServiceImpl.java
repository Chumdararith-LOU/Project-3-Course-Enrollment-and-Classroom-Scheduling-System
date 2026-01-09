package com.cource.service.impl;

import com.cource.dto.user.UserCreateRequest;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.Role;
import com.cource.entity.User;
import com.cource.repository.RoleRepository;
import com.cource.repository.UserProfileRepository;
import com.cource.repository.UserRepository;
import com.cource.service.UserService;
import com.cource.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.avatar.dir:uploads/avatars}")
    private String avatarDir;

    public User createUser(UserCreateRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setIdCard(request.getIdCard());
        user.setActive(request.isActive());

        // Set role
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);

        return userRepository.save(user);
    }

    public User updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null) {
            // Check if new email is already taken by another user
            if (!user.getEmail().equals(request.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getIdCard() != null) {
            user.setIdCard(request.getIdCard());
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role not found"));
            user.setRole(role);
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // handle profile fields: phone, dob, bio
        try {
            UserProfile profile = userProfileRepository.findByUserId(id)
                    .orElseGet(() -> {
                        UserProfile p = new UserProfile();
                        p.setUser(user);
                        return p;
                    });
            if (request.getPhone() != null) {
                profile.setPhone(request.getPhone());
            }
            if (request.getBio() != null) {
                profile.setBio(request.getBio());
            }
            if (request.getDob() != null && !request.getDob().isEmpty()) {
                // try ISO first, then dd/MM/yyyy
                LocalDate dob = null;
                try {
                    dob = LocalDate.parse(request.getDob());
                } catch (DateTimeParseException ex) {
                    try {
                        DateTimeFormatter f = DateTimeFormatter.ofPattern("d/M/yyyy");
                        dob = LocalDate.parse(request.getDob(), f);
                    } catch (DateTimeParseException ex2) {
                        // ignore invalid format
                    }
                }
                if (dob != null)
                    profile.setDateOfBirth(dob);
            }
            userProfileRepository.save(profile);
        } catch (Exception ex) {
            // do not fail whole update for profile issues
        }

        return userRepository.save(user);
    }

    public User toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateAvatar(Long id, MultipartFile file) throws IOException {
        try {
            User user = getUserById(id);
            if (file == null || file.isEmpty())
                throw new RuntimeException("No file provided");

            // ensure directory exists
            Path uploadDir = Paths.get(avatarDir).toAbsolutePath();
            System.out.println("Avatar upload directory: " + uploadDir);

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                System.out.println("Created avatar directory: " + uploadDir);
            }

            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }

            String filename = "user_" + id + "_" + System.currentTimeMillis() + ext;
            Path target = uploadDir.resolve(filename);

            System.out.println("Saving avatar to: " + target);
            file.transferTo(target.toFile());
            System.out.println("Avatar file saved successfully");

            // move avatar storage into user_profiles.avatar_url
            UserProfile profile = userProfileRepository.findByUserId(id)
                    .orElseGet(() -> {
                        UserProfile p = new UserProfile();
                        p.setUser(user);
                        return p;
                    });
            profile.setAvatarUrl(filename);
            userProfileRepository.save(profile);

            System.out.println("Avatar URL saved to profile: " + filename);
            return user;
        } catch (Exception e) {
            System.err.println("Error in updateAvatar: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Resource loadAvatarResource(Long id) throws IOException {
        // load filename from user_profiles.avatar_url
        var opt = userProfileRepository.findByUserId(id);
        if (opt.isEmpty())
            return null;
        String fn = opt.get().getAvatarUrl();
        if (fn == null || fn.isEmpty())
            return null;
        Path uploadDir = Paths.get(avatarDir).toAbsolutePath();
        Path target = uploadDir.resolve(fn);
        if (!Files.exists(target))
            return null;
        return new UrlResource(target.toUri());
    }

    @Transactional(readOnly = true)
    public com.cource.dto.user.UserProfileDTO getUserProfile(Long userId) {
        var opt = userProfileRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            return new com.cource.dto.user.UserProfileDTO(null, userId, null, null, null, null);
        }
        var p = opt.get();
        return new com.cource.dto.user.UserProfileDTO(p.getId(), userId, p.getPhone(), p.getDateOfBirth(), p.getBio(),
                p.getAvatarUrl());
    }

    @Override
    public User getUserByEmail(String email) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserByEmail'");
    }
}