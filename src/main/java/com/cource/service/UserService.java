package com.cource.service;

import com.cource.dto.user.UserCreateRequest;
import com.cource.dto.user.UserProfileDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;

public interface UserService {
    User createUser(UserCreateRequest request);

    User getUserById(Long id);

    User updateUser(Long id, UserUpdateRequest request);

    void deleteUser(Long id);

    @Transactional(readOnly = true)
    User getUserByEmail(String email);

    User updateAvatar(Long id, MultipartFile file) throws IOException;

    Resource loadAvatarResource(Long id) throws IOException;

    User toggleUserStatus(Long id);

    UserProfileDTO getUserProfile(Long id);

}