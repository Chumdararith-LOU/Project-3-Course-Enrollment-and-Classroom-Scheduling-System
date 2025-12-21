package com.cource.controller;

import com.cource.dto.course.CourseRequestDTO;
import com.cource.repository.AcademicTermRepository;
import com.cource.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.cource.service.LecturerService;


@Controller
@RequestMapping("/lecturer")
@RequiredArgsConstructor
public class LecturerViewController {

    private final LecturerService lecturerService;
    private final CourseService courseService;
    private final AcademicTermRepository termRepository;

    public LecturerViewController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping("/courses/create")
    public String showCreateCourseForm(Model model) {
        if (!model.containsAttribute("courseRequest")) {
            model.addAttribute("courseRequest", new CourseRequestDTO());
        }

        model.addAttribute("terms", termRepository.findByIsActiveTrue());

        return "views/lecturer/create_course";
    }

    @PostMapping("/courses/create")
    public String createCourse(@Valid @ModelAttribute("courseRequest") CourseRequestDTO courseRequest,
                               BindingResult result,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("terms", termRepository.findByIsActiveTrue());
            return "views/lecturer/create_course";
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            courseService.createCourse(courseRequest, email);
            return "redirect:/lecturer/courses?success=true";

        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("terms", termRepository.findByIsActiveTrue());
            return "views/lecturer/create_course";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
        }
        return "views/lecturer/dashboard";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) Long lecturerId, Model model) {
        // TODO: After enabling security, get lecturerId from Authentication
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("courses", lecturerService.getCoursesByLecturerId(lecturerId));
        }
        return "views/lecturer/courses";
    }

    @GetMapping("/students")
    public String students(
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) Long lecturerId,
            Model model) {
        // TODO: After enabling security, get lecturerId from Authentication
        if (offeringId != null && lecturerId != null) {
            model.addAttribute("offeringId", offeringId);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("students", lecturerService.getEnrolledStudents(offeringId, lecturerId));
        }
        return "views/lecturer/students";
    }

    @GetMapping("/attendance")
    public String attendance(
            @RequestParam(required = false) Long scheduleId,
            @RequestParam(required = false) Long lecturerId,
            Model model) {
        // TODO: After enabling security, get lecturerId from Authentication
        if (scheduleId != null && lecturerId != null) {
            model.addAttribute("scheduleId", scheduleId);
            model.addAttribute("lecturerId", lecturerId);
            model.addAttribute("attendanceRecords", lecturerService.getAttendanceRecords(scheduleId, lecturerId));
        }
        return "views/lecturer/attendance";
    }

    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
        }
        return "views/lecturer/schedule";
    }

    @GetMapping("/reports")
    public String reports(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId != null) {
            model.addAttribute("lecturerId", lecturerId);
        }
        return "views/lecturer/reports";
    }
}
