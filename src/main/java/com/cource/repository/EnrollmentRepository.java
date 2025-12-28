package com.cource.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cource.entity.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByStudentIdAndGradeIsNotNull(Long studentId);

    List<Enrollment> findByOfferingId(Long offeringId);

    long countByOfferingId(Long offeringId);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.offering.id = :offeringId")
    Optional<Enrollment> findByStudentIdAndOfferingId(@Param("studentId") Long studentId,
            @Param("offeringId") Long offeringId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.offering.id = :offeringId AND e.status = :status")
    long countByOfferingIdAndStatus(@Param("offeringId") Long offeringId, @Param("status") String status);

    long countByOffering_Term_Id(Long termId);

    long countByOffering_Course_Id(Long courseId);

}
