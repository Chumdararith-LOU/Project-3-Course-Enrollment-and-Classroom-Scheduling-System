package com.cource.repository;

import com.cource.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    int countByOfferingIdAndStatus(Long offeringId, String status);
    boolean existsByStudentIdAndOfferingId(Long studentId, Long offeringId);
    long countByStudentIdAndStatus(Long studentId, String status);
}
