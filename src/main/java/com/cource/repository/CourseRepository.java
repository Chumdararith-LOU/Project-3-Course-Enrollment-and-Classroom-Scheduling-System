package com.cource.repository;

import com.cource.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByCourseCode(String courseCode);

    List<Course> findByActiveTrue();

    Optional<Course> findByCourseCode(String courseCode);

    List<Course> findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(String courseCode, String title);
}