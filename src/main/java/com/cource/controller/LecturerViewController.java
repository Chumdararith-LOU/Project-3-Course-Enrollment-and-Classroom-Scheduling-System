package com.cource.controller;

import com.cource.dto.course.CourseRequestDTO;
import com.cource.dto.course.CourseResponseDTO;
import com.cource.entity.User;
import com.cource.repository.AcademicTermRepository;
import com.cource.repository.UserRepository;
import com.cource.service.CourseService;
import com.cource.service.LecturerService;
import com.cource.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/lecturer")
@RequiredArgsConstructor
public class LecturerViewController {

    private final LecturerService lecturerService;
    private final CourseService courseService;
    private final AcademicTermRepository termRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping("/courses")
    public String myCourses(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUserByDetails(userDetails);
        if (user != null) {
            List<CourseResponseDTO> courses = lecturerService.getCoursesByLecturerId(user.getId());
            model.addAttribute("courses", courses);
            model.addAttribute("lecturerId", user.getId());
        }
        return "lecturer/courses";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUserByDetails(userDetails);
        if (user != null) {
            model.addAttribute("lecturerId", user.getId());
            model.addAttribute("stats", lecturerService.getDashboardStats(user.getId()));
        }
        return "lecturer/dashboard";
    }

    @GetMapping("/courses/create")
    public String showCreateCourseForm(Model model) {
        if (!model.containsAttribute("courseRequest")) {
            model.addAttribute("courseRequest", new CourseRequestDTO());
        }
        model.addAttribute("terms", termRepository.findByActiveTrue());
        return "lecturer/create_course";
    }

    @PostMapping("/courses/create")
    public String createCourse(@Valid @ModelAttribute("courseRequest") CourseRequestDTO courseRequest,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("terms", termRepository.findByActiveTrue());
            return "lecturer/create_course";
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            courseService.createCourse(courseRequest, email);
            return "redirect:/lecturer/courses";

        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("terms", termRepository.findByActiveTrue());
            return "lecturer/create_course";
        }
    }

    @GetMapping("/students")
    public String students(
            @RequestParam(required = false) Long offeringId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = getUserByDetails(userDetails);

        if (offeringId != null && user != null) {
            model.addAttribute("offeringId", offeringId);
            model.addAttribute("lecturerId", user.getId());
            model.addAttribute("students", lecturerService.getEnrolledStudents(offeringId, user.getId()));
        }
        return "lecturer/students";
    }

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(required = false) Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = getUserByDetails(userDetails);

        if (scheduleId != null && user != null) {
            model.addAttribute("scheduleId", scheduleId);
            model.addAttribute("lecturerId", user.getId());
            model.addAttribute("attendanceRecords", lecturerService.getAttendanceRecords(scheduleId, user.getId()));
        }
        return "lecturer/attendance";
    }

    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUserByDetails(userDetails);
        if (user != null) {
            model.addAttribute("lecturerId", user.getId());
        }
        return "lecturer/schedule";
    }

    @GetMapping("/reports")
    public String reports(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getUserByDetails(userDetails);
        if (user != null) {
            model.addAttribute("lecturerId", user.getId());
        }
        return "lecturer/reports";
    }

    private User getUserByDetails(UserDetails userDetails) {
        if (userDetails != null) {
            return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }
        return null;
    }
}