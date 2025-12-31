package com.cource.service;

import com.cource.dto.user.UserCreateRequest;
import com.cource.dto.user.UserProfileDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User getUserByEmail(String email);

    User getUserById(Long id);

    User createUser(UserCreateRequest request);

    User updateUser(Long id, UserUpdateRequest request);

    User toggleUserStatus(Long id);

    void deleteUser(Long id);

    void updateProfile(Long userId, UserProfileDTO dto, MultipartFile avatar);
}