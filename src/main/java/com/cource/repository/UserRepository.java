package com.cource.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cource.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    List<User> findByRole_RoleName(String roleName);

    List<User> findByRole_RoleCode(String roleCode);

    long countByRole_RoleName(String roleName);

    boolean existsByEmail(String email);
}
