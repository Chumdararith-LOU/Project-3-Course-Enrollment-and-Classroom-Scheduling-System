package com.cource.controller;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.user.UserResponseDTO;
import com.cource.service.CourseService;
import com.cource.service.EnrollmentService;
import com.cource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long userId = getCurrentUserId();

        UserResponseDTO user = userService.getUserById(userId);
        model.addAttribute("user", user);

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

        UserResponseDTO user = userService.getUserById(userId);
        model.addAttribute("user", user);

        List<CourseResponseDTO> courses = courseService.getCatalogForStudent(userId);
        model.addAttribute("courses", courses);

        model.addAttribute("currentPage", "catalog");
        return "student/catalog";
    }

    @GetMapping("/my-courses")
    public String myCourses(Model model) {
        Long userId = getCurrentUserId();
        UserResponseDTO user = userService.getUserById(userId);
        model.addAttribute("user", user);

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
}