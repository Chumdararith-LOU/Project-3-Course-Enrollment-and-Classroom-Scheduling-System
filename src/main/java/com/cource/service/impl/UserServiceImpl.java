package com.cource.service.impl;

import com.cource.dto.user.UserResponseDTO;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.UserRepository;
import com.cource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Map Entity to DTO
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());

        // Handle Role (assuming Role entity has getRoleName or getRoleCode)
        if (user.getRole() != null) {
            dto.setRole(user.getRole().getRoleName());
        }

        return dto;
    }
}
