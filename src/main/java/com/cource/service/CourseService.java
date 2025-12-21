package com.cource.service;

import com.cource.dto.course.CourseResponseDTO;

import java.util.List;

public interface CourseService {
    List<CourseResponseDTO> getCatalogForStudent(Long studentId);
}
