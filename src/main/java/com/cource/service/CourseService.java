package com.cource.service;

import com.cource.dto.course.CourseRequestDTO;
import com.cource.dto.course.CourseResponseDTO;

import java.util.List;

public interface CourseService {
    List<CourseResponseDTO> getCatalogForStudent(Long studentId);

    void createCourse(CourseRequestDTO dto, String lecturerEmail);
    List<CourseResponseDTO> getCoursesByLecturerId(Long lecturerId);
}