package com.cource.repository;

import com.cource.entity.CourseLecturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseLecturerRepository extends JpaRepository<CourseLecturer, Long> {
    Optional<CourseLecturer> findByOfferingIdAndPrimaryTrue(Long offeringId);
}
