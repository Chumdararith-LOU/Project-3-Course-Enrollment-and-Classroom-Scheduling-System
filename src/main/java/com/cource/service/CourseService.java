package com.cource.service;

import com.cource.dto.course.CourseCreateRequest;
import com.cource.dto.course.CourseUpdateRequest;
import com.cource.entity.Course;
import com.cource.entity.User;

import java.util.List;

public interface CourseService {
    List<Course> getAllCourses();

    Course getCourseById(Long id);
    Course createCourse(CourseCreateRequest request);
    Course updateCourse(Long id, CourseUpdateRequest request);

    void deleteCourse(Long id);

    Course toggleCourseStatus(Long id);
    List<Course> searchCourses(String keyword);
    String generateEnrollmentCode(String courseCode);
}