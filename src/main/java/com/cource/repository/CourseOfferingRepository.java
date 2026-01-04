package com.cource.repository;

import com.cource.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    @Query("SELECT co FROM CourseOffering co JOIN co.term t WHERE co.active = true AND t.active = true")
    List<CourseOffering> findAllActiveOfferings();

    List<CourseOffering> findByTermId(Long termId);

    List<CourseOffering> findByCourseId(Long courseId);

    List<CourseOffering> findByActive(Boolean active);

    List<CourseOffering> findByActiveAndTermId(Boolean active, Long termId);

    Optional<CourseOffering> findByEnrollmentCode(String enrollmentCode);

    boolean existsByEnrollmentCode(String enrollmentCode);

    @Query("SELECT co FROM CourseOffering co WHERE co.course.id = :courseId AND co.term.id = :termId")
    Optional<CourseOffering> findByCourseIdAndTermId(@Param("courseId") Long courseId, @Param("termId") Long termId);

    @Query("SELECT co FROM CourseOffering co WHERE co.term.id = :termId AND co.active = true")
    List<CourseOffering> findActiveByTermId(@Param("termId") Long termId);

    @Query("SELECT co FROM CourseOffering co WHERE LOWER(co.course.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<CourseOffering> findByCourseTitleContainingIgnoreCase(@Param("keyword") String keyword);

    @Query("SELECT co FROM CourseOffering co WHERE LOWER(co.course.title) LIKE LOWER(CONCAT('%', :keyword, '%')) AND co.term.id = :termId")
    List<CourseOffering> findByCourseTitleContainingIgnoreCaseAndTermId(@Param("keyword") String keyword,
            @Param("termId") Long termId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.offering.id = :offeringId AND e.status = 'ENROLLED'")
    Long countEnrolledStudents(@Param("offeringId") Long offeringId);

    @Query("SELECT co FROM CourseOffering co LEFT JOIN FETCH co.course LEFT JOIN FETCH co.term WHERE co.id = :id")
    Optional<CourseOffering> findByIdWithDetails(@Param("id") Long id);
}