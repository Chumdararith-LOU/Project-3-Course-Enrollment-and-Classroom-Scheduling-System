package com.cource.controller;

import com.cource.dto.course.CourseCatalogDTO;
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

import java.util.ArrayList;
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

        long enrolledCount = enrollmentService.getEnrolledCourseCount(userId);

        model.addAttribute("user", user);
        model.addAttribute("enrolledCount", enrolledCount);

        model.addAttribute("todaysClasses", java.util.Collections.emptyList());

        model.addAttribute("currentPage", "dashboard");

        return "student/dashboard";
    }

    @GetMapping("/catalog")
    public String courseCatalog(Model model) {
        Long userId = getCurrentUserId();
        List<CourseResponseDTO> courses = courseService.getCatalogForStudent(userId);

        model.addAttribute("courses", courses);
        model.addAttribute("currentPage", "catalog");

        return "student/catalog";
    }

    @GetMapping("/my-courses")
    public String myCourses(Model model) {
        model.addAttribute("currentPage", "my-courses");
        return "student/my-courses";
    }

}