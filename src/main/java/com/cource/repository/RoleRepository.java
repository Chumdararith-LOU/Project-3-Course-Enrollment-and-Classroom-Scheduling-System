package com.cource.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cource.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleCode(String roleCode);

    Optional<Role> findByRoleName(String roleName);
}
