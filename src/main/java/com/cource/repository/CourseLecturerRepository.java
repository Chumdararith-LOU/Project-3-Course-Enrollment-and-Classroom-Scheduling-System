package com.cource.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cource.entity.Course;
import com.cource.entity.CourseLecturer;

public interface CourseLecturerRepository extends JpaRepository<CourseLecturer, Long> {
    List<CourseLecturer> findByLecturerId(Long lecturerId);
    
    @Query("SELECT cl FROM CourseLecturer cl WHERE cl.offering.id = :offeringId AND cl.lecturer.id = :lecturerId")
    boolean existsByOfferingIdAndLecturerId(@Param("offeringId") Long offeringId, @Param("lecturerId") Long lecturerId);

}
