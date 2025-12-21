package com.cource.service;

import com.cource.dto.course.CourseRequestDTO;
import com.cource.dto.course.CourseResponseDTO;

import java.util.List;

public interface CourseService {
    void createCourse(CourseRequestDTO dto, String lecturerEmail);

    // Existing methods (ensure these match what you need)
    List<CourseResponseDTO> getCatalogForStudent(Long studentId);
    List<CourseResponseDTO> getCoursesByLecturerId(Long lecturerId);
}
