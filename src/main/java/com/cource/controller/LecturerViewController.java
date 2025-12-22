package com.cource.controller;

import com.cource.dto.course.CourseRequestDTO;
import com.cource.entity.User;
import com.cource.repository.AcademicTermRepository;
import com.cource.repository.UserRepository;
import com.cource.service.CourseService;
import com.cource.service.LecturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/lecturer")
@RequiredArgsConstructor
public class LecturerViewController {

    private final LecturerService lecturerService;
    private final CourseService courseService;
    private final AcademicTermRepository termRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) {
            lecturerId = getCurrentLecturerId();
        }

        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("stats", lecturerService.getDashboardStats(lecturerId));
        }
        return "lecturer/dashboard";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) {
            lecturerId = getCurrentLecturerId();
        }

        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("courses", courseService.getCoursesByLecturerId(lecturerId));
        }
        return "lecturer/courses";
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
            @RequestParam(required = false) Long lecturerId,
            Model model) {
        if (lecturerId == null) {
            lecturerId = getCurrentLecturerId();
        }

        if (offeringId != null && lecturerId != null) {
            model.addAttribute("offeringId", offeringId);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("students", lecturerService.getEnrolledStudents(offeringId, lecturerId));
        }
        return "lecturer/students";
    }

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long lecturerId,
            Model model) {
        if (lecturerId == null) {
            lecturerId = getCurrentLecturerId();
        }

        if (scheduleId != null && lecturerId != null) {
            model.addAttribute("scheduleId", scheduleId);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("attendanceRecords", lecturerService.getAttendanceRecords(scheduleId, lecturerId));
        }
        return "lecturer/attendance";
    }

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) {
            lecturerId = getCurrentLecturerId();
        }

        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
        }
        return "lecturer/schedule";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) {
            lecturerId = getCurrentLecturerId();
        }

        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
        }
        return "lecturer/reports";
    }

    private Long getCurrentLecturerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return userRepository.findByEmail(auth.getName())
                    .map(User::getId)
                    .orElse(null);
        }
        return null;
    }
}