package com.cource.service;

import com.cource.dto.user.UserResponseDTO;

public interface UserService {
    UserResponseDTO getUserById(Long id);
}
