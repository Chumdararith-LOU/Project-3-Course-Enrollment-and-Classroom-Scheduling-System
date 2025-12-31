package com.cource.service;

import com.cource.dto.course.CourseCreateRequest;
import com.cource.dto.course.CourseRequestDTO;
import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.course.CourseUpdateRequest;
import com.cource.dto.enrollment.StudentEnrollmentDTO;
import com.cource.entity.Course;

import java.util.List;

public interface CourseService {

    List<CourseResponseDTO> getCatalogForStudent(Long studentId);
    List<StudentEnrollmentDTO> getStudentEnrollments(Long studentId);

    void createCourse(CourseRequestDTO dto, String lecturerEmail);
    List<CourseResponseDTO> getCoursesByLecturerId(Long lecturerId);

    List<Course> searchCourses(String search);
    Course getCourseById(Long id);
    Course createCourse(CourseCreateRequest request);
    Course updateCourse(Long id, CourseUpdateRequest request);
    void toggleCourseStatus(Long id);
    void regenerateEnrollmentCode(Long id);
}