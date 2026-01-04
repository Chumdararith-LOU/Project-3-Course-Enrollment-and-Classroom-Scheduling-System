package com.cource.controller;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.lecturer.LecturerDashboardDTO;
import com.cource.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cource.service.LecturerService;
import java.util.List;

@Controller
@RequestMapping("/lecturer")
@RequiredArgsConstructor
public class LecturerViewController {

    private final LecturerService lecturerService;
    private final SecurityHelper securityHelper;

    @GetMapping("/dashboard")
    public String getDashboard(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) {
            lecturerId = securityHelper.getCurrentUserId();
        }

        LecturerDashboardDTO stats = lecturerService.getDashboardStats(lecturerId);

        model.addAttribute("dashboard", stats);
        model.addAttribute("lecturerId", lecturerId);

        return "lecturer/dashboard";
    }

    @GetMapping("/schedule")
    public String getSchedulePage(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) lecturerId = securityHelper.getCurrentUserId();

        model.addAttribute("lecturerId", lecturerId);
        return "lecturer/schedule";
    }

    @GetMapping("/attendance")
    public String getAttendancePage(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) lecturerId = securityHelper.getCurrentUserId();

        model.addAttribute("lecturerId", lecturerId);
        return "lecturer/attendance";
    }

    @GetMapping("/reports")
    public String getReportsPage(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) lecturerId = securityHelper.getCurrentUserId();

        model.addAttribute("lecturerId", lecturerId);
        return "lecturer/reports";
    }

    @GetMapping("/courses")
    public String getCoursesPage(@RequestParam(required = false) Long lecturerId, Model model) {
        if (lecturerId == null) lecturerId = securityHelper.getCurrentUserId();

        List<CourseResponseDTO> courses = lecturerService.getCoursesByLecturerId(lecturerId);
        model.addAttribute("courses", courses);
        model.addAttribute("lecturerId", lecturerId);
        return "lecturer/courses";
    }
}