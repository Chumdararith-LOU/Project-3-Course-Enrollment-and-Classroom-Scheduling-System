package com.cource.controller;

import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.enrollment.StudentEnrollmentDTO;
import com.cource.dto.schedule.ScheduleResponseDTO;
import com.cource.dto.user.UserResponseDTO;
import com.cource.dto.user.UserUpdateRequest;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.repository.EnrollmentRepository;
import com.cource.service.CourseService;
import com.cource.service.EnrollmentService;
import com.cource.service.ScheduleService;
import com.cource.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final CourseService courseService;
    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;
    private final ScheduleService scheduleService;

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
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model){
        User user = getUserByDetails(userDetails);

        model.addAttribute("user", user);
        model.addAttribute("enrolledCount", enrollmentService.getEnrolledCourseCount(user.getId()));

        model.addAttribute("enrollments", courseService.getStudentEnrollments(user.getId()));
        return "student/dashboard";
    }

    @GetMapping("/catalog")
    public String catalog(@AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam(required = false) String search,
                          Model model) {
        User user = getUserByDetails(userDetails);
        model.addAttribute("user", user);

        List<CourseResponseDTO> courses = courseService.getCatalogForStudent(user.getId());
        model.addAttribute("courses", courses);
        return "student/catalog";
    }

    @GetMapping("/my-courses")
    public String myCourses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = getUserByDetails(userDetails);
        model.addAttribute("user", student);

        List<StudentEnrollmentDTO> allEnrollments = courseService.getStudentEnrollments(student.getId());

        List<StudentEnrollmentDTO> active = allEnrollments.stream()
                .filter(e -> "ENROLLED".equalsIgnoreCase(e.getStatus()))
                .collect(Collectors.toList());

        List<StudentEnrollmentDTO> waitlist = allEnrollments.stream()
                .filter(e -> "WAITLIST".equalsIgnoreCase(e.getStatus()) || "WAITLISTED".equalsIgnoreCase(e.getStatus()))
                .collect(Collectors.toList());

        List<StudentEnrollmentDTO> completed = allEnrollments.stream()
                .filter(e -> "COMPLETED".equalsIgnoreCase(e.getStatus()))
                .collect(Collectors.toList());

        List<StudentEnrollmentDTO> dropped = allEnrollments.stream()
                .filter(e -> "DROPPED".equalsIgnoreCase(e.getStatus()))
                .collect(Collectors.toList());

        model.addAttribute("activeEnrollments", active);
        model.addAttribute("waitlistEnrollments", waitlist);
        model.addAttribute("completedEnrollments", completed);
        model.addAttribute("droppedEnrollments", dropped);

        return "student/my-courses";
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponseDTO>> getCourses(@RequestParam(required = false) Long studentId) {
        Long id = (studentId != null) ? studentId : getCurrentUserId();
        return ResponseEntity.ok(courseService.getCatalogForStudent(id));
    }

    @GetMapping("/schedule")
    public String schedule(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = getUserByDetails(userDetails);
        model.addAttribute("user", student);
        List<ScheduleResponseDTO> schedules = scheduleService.getStudentSchedule(student.getId());
        model.addAttribute("schedules", schedules);
        model.addAttribute("currentPage", "schedule");
        return "student/schedule";
    }

    @GetMapping("/grades")
    public String grades(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = getUserByDetails(userDetails);
        model.addAttribute("user", student);

        List<Enrollment> grades = enrollmentService.getStudentGrades(student.getId());
        model.addAttribute("grades", grades);
        model.addAttribute("currentPage", "grades");
        return "student/grades";
    }

    @GetMapping("/attendance")
    public String attendance(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User student = getUserByDetails(userDetails);
        model.addAttribute("user", student);
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

    private User getUserByDetails(UserDetails userDetails) {
        return userService.getUserByEmail(userDetails.getUsername());
    }
}