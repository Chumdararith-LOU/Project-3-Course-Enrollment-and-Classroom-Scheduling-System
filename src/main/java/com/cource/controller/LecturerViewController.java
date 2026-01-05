package com.cource.controller;

import com.cource.dto.course.CourseRequestDTO;
import com.cource.dto.course.CourseResponseDTO;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.exception.ResourceNotFoundException;
import com.cource.repository.AcademicTermRepository;
import com.cource.repository.CourseOfferingRepository;
import com.cource.repository.UserRepository;
import com.cource.service.CourseService;
import com.cource.service.EnrollmentService;
import com.cource.service.LecturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final EnrollmentService enrollmentService;
    private final CourseOfferingRepository courseOfferingRepository;

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
        model.addAttribute("course", new CourseRequestDTO());
        model.addAttribute("terms", termRepository.findByActiveTrue());
        return "lecturer/create_course";
    }

    @GetMapping("/students")
    public String viewEnrolledStudents(@RequestParam(required = false) Long offeringId,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User user = getUserByDetails(userDetails);

        if (offeringId != null) {
            CourseOffering offering = courseOfferingRepository.findById(offeringId)
                    .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));

            List<Enrollment> enrollments = enrollmentService.getEnrollmentsByOffering(offeringId);

            model.addAttribute("offering", offering);
            model.addAttribute("enrollments", enrollments);
        }

        if (user != null) {
            model.addAttribute("lecturerId", user.getId());
            model.addAttribute("user", user);
        }

        return "lecturer/students";
    }

    @PostMapping("/courses/create")
    public String createCourse(@Valid @ModelAttribute("course") CourseRequestDTO courseRequest,
            BindingResult result,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("terms", termRepository.findByActiveTrue());
            return "lecturer/create_course";
        }

        courseService.createCourse(courseRequest, userDetails.getUsername());

        return "redirect:/lecturer/courses";
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

    @PostMapping("/grades/update")
    public String updateGrade(@RequestParam Long enrollmentId,
            @RequestParam Long offeringId,
            @RequestParam String grade) {

        enrollmentService.updateGrade(enrollmentId, grade);

        return "redirect:/lecturer/students?offeringId=" + offeringId;
    }

    private User getUserByDetails(UserDetails userDetails) {
        if (userDetails != null) {
            return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

}