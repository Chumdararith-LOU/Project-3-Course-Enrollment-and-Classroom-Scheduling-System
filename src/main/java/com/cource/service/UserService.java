package com.cource.service;

import com.cource.dto.user.UserCreateRequest;
import com.cource.dto.user.UserProfileDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UserService {
    User createUser(UserCreateRequest request);
    User updateUser(Long id, UserUpdateRequest request);
    User toggleUserStatus(Long id);
    void deleteUser(Long id);
    User getUserById(Long id);
    User getUserByEmail(String email);
    User updateAvatar(Long id, MultipartFile file) throws IOException;
    Resource loadAvatarResource(Long id) throws IOException;
    UserProfileDTO getUserProfile(Long userId);
}