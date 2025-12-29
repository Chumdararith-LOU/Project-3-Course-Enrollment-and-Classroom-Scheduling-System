package com.cource.controller;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.user.UserResponseDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.repository.EnrollmentRepository;
import com.cource.service.CourseService;
import com.cource.service.EnrollmentService;
import com.cource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseService courseService;
    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;

    private Long getCurrentUserId() {
        return 3L;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getProfile(Authentication authentication) {
        User user = userService.getUserById(getCurrentUserId());
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @GetMapping("/info/{id}")
    public ResponseEntity<UserResponseDTO> getStudentById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateProfile(@RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(getCurrentUserId(), request);
        return ResponseEntity.ok(mapToResponseDTO(updatedUser));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long userId = getCurrentUserId();

        // User Info
        User user = userService.getUserById(userId);
        model.addAttribute("user", mapToResponseDTO(user));

        // Stats
        long enrolledCount = enrollmentService.getEnrolledCourseCount(userId);
        model.addAttribute("enrolledCount", enrolledCount);

        // Recent Enrollments (limit to 5 for dashboard)
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(userId);
        model.addAttribute("enrollments", enrollments);

        List<CourseResponseDTO> courses = courseService.getCatalogForStudent(userId);
        model.addAttribute("courses", courses);

        model.addAttribute("todaysClasses", java.util.Collections.emptyList());
        model.addAttribute("currentPage", "dashboard");

        return "student/dashboard";
    }

    @GetMapping("/catalog")
    public String courseCatalog(Model model) {
        Long userId = getCurrentUserId();
        User user = userService.getUserById(userId);

        model.addAttribute("user", mapToResponseDTO(user));
        model.addAttribute("courses", courseService.getCatalogForStudent(userId));
        model.addAttribute("currentPage", "catalog");
        return "student/catalog";
    }

    @GetMapping("/my-courses")
    public String myCourses(Model model) {
        Long userId = getCurrentUserId();

        User user = userService.getUserById(userId);

        // Fetch all enrollments for this student
        List<Enrollment> allEnrollments = enrollmentRepository.findByStudentId(userId);

        // Filter by status for specific tabs if needed, or pass all and filter in Thymeleaf
        model.addAttribute("enrollments", allEnrollments);
        model.addAttribute("user", mapToResponseDTO(user));

        model.addAttribute("currentPage", "my-courses");

        return "student/my-courses";
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponseDTO>> getCourses(@RequestParam(required = false) Long studentId) {
        // Use provided ID or fallback to current user
        Long id = (studentId != null) ? studentId : getCurrentUserId();
        return ResponseEntity.ok(courseService.getCatalogForStudent(id));
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        Long userId = getCurrentUserId();
        model.addAttribute("studentId", userId); // Needed for sidebar links
        model.addAttribute("currentPage", "schedule");
        return "student/schedule";
    }

    @GetMapping("/grades")
    public String grades(Model model) {
        Long userId = getCurrentUserId();
        model.addAttribute("studentId", userId);

        // Fetch graded enrollments
        List<Enrollment> grades = enrollmentRepository.findByStudentIdAndGradeIsNotNull(userId);
        model.addAttribute("grades", grades);
        model.addAttribute("currentPage", "grades");
        return "student/grades";
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
        Long userId = getCurrentUserId();
        model.addAttribute("studentId", userId);
        model.addAttribute("currentPage", "attendance");
        model.addAttribute("currentPage", "attendance");
        return "student/attendance";
    }

    private UserResponseDTO mapToResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .idCard(user.getIdCard())
                .role(user.getRole().getRoleName())
                .isActive(user.isActive())
                .build();

    }
}