package com.cource.service.impl;

import com.cource.dto.user.UserCreateRequest;
import com.cource.dto.user.UserProfileDTO;
import com.cource.dto.user.UserResponseDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.Role;
import com.cource.entity.User;
import com.cource.entity.UserProfile;
import com.cource.exception.ConflictException;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.RoleRepository;
import com.cource.repository.UserRepository;
import com.cource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setIdCard(request.getIdCard());
        user.setActive(request.isActive());

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        user.setRole(role);

        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = getUserById(id);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email already exists");
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

        // Update active status regardless of null check if it's a primitive boolean in DTO,
        // but assuming wrapper or direct set from request logic:
        user.setActive(request.isActive());

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
            user.setRole(role);
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    @Override
    public User toggleUserStatus(Long id) {
        User user = getUserById(id);
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileDTO dto, MultipartFile avatar) {
        User user = getUserById(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(new UserProfile());

        if (profile.getUser() == null) {
            profile.setUser(user);
        }

        if (avatar != null && !avatar.isEmpty()) {
            String fileName = fileStorageService.storeFile(avatar);
            profile.setAvatarUrl("/uploads/" + fileName);
        }

        if (dto.getBio() != null) profile.setBio(dto.getBio());
        if (dto.getPhone() != null) profile.setPhone(dto.getPhone());
        if (dto.getDateOfBirth() != null) profile.setDateOfBirth(dto.getDateOfBirth());

        userProfileRepository.save(profile);
    }
}
