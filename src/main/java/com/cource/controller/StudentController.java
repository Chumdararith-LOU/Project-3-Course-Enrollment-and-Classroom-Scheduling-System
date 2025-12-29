package com.cource.controller;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.user.UserResponseDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.User;
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
    private final EnrollmentService enrollmentService;

    private Long getCurrentUserId() {
        return 3L;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getProfile(Authentication authentication) {
        Long userId = (authentication != null) ? 0L : getCurrentUserId();
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getStudentById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(mapToResponseDTO(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateProfile(@RequestBody UserUpdateRequest request, Authentication authentication) {
        Long userId = (authentication != null) ? 0L : getCurrentUserId();
        User updatedUser = userService.updateUser(userId, request);
        return ResponseEntity.ok(mapToResponseDTO(updatedUser));
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long userId = getCurrentUserId();

        User userEntity = userService.getUserById(userId);
        UserResponseDTO userDTO = mapToResponseDTO(userEntity);
        model.addAttribute("user", userDTO);

        long enrolledCount = enrollmentService.getEnrolledCourseCount(userId);
        model.addAttribute("enrolledCount", enrolledCount);

        List<CourseResponseDTO> courses = courseService.getCatalogForStudent(userId);
        model.addAttribute("courses", courses);

        model.addAttribute("todaysClasses", java.util.Collections.emptyList());
        model.addAttribute("currentPage", "dashboard");

        return "student/dashboard";
    }

    @GetMapping("/catalog")
    public String courseCatalog(Model model) {
        Long userId = getCurrentUserId();

        User userEntity = userService.getUserById(userId);
        UserResponseDTO userDTO = mapToResponseDTO(userEntity);
        model.addAttribute("user", userDTO);

        List<CourseResponseDTO> courses = courseService.getCatalogForStudent(userId);
        model.addAttribute("courses", courses);

        model.addAttribute("currentPage", "catalog");
        return "student/catalog";
    }

    @GetMapping("/my-courses")
    public String myCourses(Model model) {
        Long userId = getCurrentUserId();

        User userEntity = userService.getUserById(userId);
        UserResponseDTO userDTO = mapToResponseDTO(userEntity);
        model.addAttribute("user", userDTO);

        List<CourseResponseDTO> courses = courseService.getCatalogForStudent(userId);
        model.addAttribute("courses", courses);

        model.addAttribute("currentPage", "my-courses");

        return "student/my-courses";
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        model.addAttribute("currentPage", "schedule");
        return "student/schedule";
    }

    @GetMapping("/grades")
    public String grades(Model model) {
        model.addAttribute("currentPage", "grades");
        return "student/grades";
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
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