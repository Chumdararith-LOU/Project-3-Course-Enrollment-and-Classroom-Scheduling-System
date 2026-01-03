package com.cource.repository;

import java.util.List;
import com.cource.entity.CourseLecturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseLecturerRepository extends JpaRepository<CourseLecturer, Long> {
    List<CourseLecturer> findByLecturerId(Long lecturerId);
    List<CourseLecturer> findByOfferingIdAndPrimaryTrue(Long offeringId);

    @Query("SELECT CASE WHEN COUNT(cl) > 0 THEN true ELSE false END FROM CourseLecturer cl WHERE cl.offering.id = :offeringId AND cl.lecturer.id = :lecturerId")
    boolean existsByOfferingIdAndLecturerId(@Param("offeringId") Long offeringId, @Param("lecturerId") Long lecturerId);
}
