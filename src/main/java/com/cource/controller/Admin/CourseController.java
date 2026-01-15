
package com.cource.controller.Admin;

import com.cource.dto.course.CourseCreateRequest;
import com.cource.dto.course.CourseUpdateRequest;
import com.cource.entity.Course;
import com.cource.entity.User;
import com.cource.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CourseController {
    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CourseCreateRequest request) {
        Course course = courseService.createCourse(request);
        return ResponseEntity.ok(course);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody CourseUpdateRequest request) {
        Course course = courseService.updateCourse(id, request);
        return ResponseEntity.ok(course);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/lecturers")
    public ResponseEntity<Void> assignLecturers(@PathVariable Long id, @RequestBody List<Long> lecturerIds) {
        courseService.assignLecturersToCourse(id, lecturerIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/lecturers")
    public ResponseEntity<List<User>> getLecturers(@PathVariable Long id) {
        List<User> lecturers = courseService.getLecturersForCourse(id);
        return ResponseEntity.ok(lecturers);
    }
}
