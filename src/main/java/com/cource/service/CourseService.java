package com.cource.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cource.dto.course.CourseCreateRequest;
import com.cource.dto.course.CourseUpdateRequest;
import com.cource.entity.Course;
import com.cource.repository.CourseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
@SuppressWarnings("null")
public class CourseService {
    private final CourseRepository courseRepository;

    public List<Course> searchCourses(String search) {
        return courseRepository.findByCourseCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(search, search);
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }

    public Course createCourse(CourseCreateRequest request) {
        Course course = new Course();
        course.setCourseCode(request.getCourseCode());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCredits(request.getCapacity());
        course.setActive(request.isActive());
        return courseRepository.save(course);
    }

    public Course updateCourse(Long id, CourseUpdateRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (request.getCourseCode() != null) {
            course.setCourseCode(request.getCourseCode());
        }
        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getCapacity() > 0) {
            course.setCredits(request.getCapacity());
        }

        return courseRepository.save(course);
    }

    public void toggleCourseStatus(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setActive(!course.isActive());
        courseRepository.save(course);
    }

    public void regenerateEnrollmentCode(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        // Generate new enrollment code (simple implementation)
        // Note: enrollmentCode field doesn't exist in Course entity
        // This method may need adjustment based on actual requirements
        courseRepository.save(course);
    }
}
