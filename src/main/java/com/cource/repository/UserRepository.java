package com.cource.repository;

import com.cource.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdCard(String idCard);

    List<User> findByRole_RoleName(String roleName);

    List<User> findByRole_RoleCode(String roleCode);

    long countByRole_RoleName(String roleName);
}