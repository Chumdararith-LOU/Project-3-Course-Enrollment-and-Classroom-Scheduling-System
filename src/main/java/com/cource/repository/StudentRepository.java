package com.cource.repository;

import com.cource.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    // Find student by email
    Optional<Student> findByEmail(String email);
    
    // Find student by student ID (id_card)
    Optional<Student> findByStudentId(String studentId);
    
    // Check if email exists
    boolean existsByEmail(String email);
    
    // Check if student ID exists
    boolean existsByStudentId(String studentId);
    
    // Find active students only
    List<Student> findByIsActiveTrue();
    
    // Find students by role
    @Query("SELECT s FROM Student s WHERE s.role.roleCode = :roleCode")
    List<Student> findByRoleCode(@Param("roleCode") String roleCode);
    
    // Find active students by role
    @Query("SELECT s FROM Student s WHERE s.role.roleCode = :roleCode AND s.isActive = true")
    List<Student> findActiveByRoleCode(@Param("roleCode") String roleCode);
    
    // Search students by name
    @Query("SELECT s FROM Student s WHERE " +
           "(LOWER(s.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "s.isActive = true")
    List<Student> findActiveByNameContaining(@Param("name") String name);
    
    // Count active students by role
    @Query("SELECT COUNT(s) FROM Student s WHERE s.role.roleCode = :roleCode AND s.isActive = true")
    long countActiveByRoleCode(@Param("roleCode") String roleCode);
}
