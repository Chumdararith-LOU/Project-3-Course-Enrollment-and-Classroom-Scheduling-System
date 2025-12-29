package com.cource.service;

import com.cource.dto.user.UserCreateRequest;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.User;

public interface UserService {

    User getUserById(Long id);

    User createUser(UserCreateRequest request);

    User updateUser(Long id, UserUpdateRequest request);

    User toggleUserStatus(Long id);

    void deleteUser(Long id);
}