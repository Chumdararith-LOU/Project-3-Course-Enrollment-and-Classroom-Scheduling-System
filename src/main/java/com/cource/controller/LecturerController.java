package com.cource.controller;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.course.CourseResponseDTO;
import com.cource.exception.ResourceNotFoundException;
import com.cource.exception.UnauthorizedException;
import com.cource.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.User;
import com.cource.service.LecturerService;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/lecturer")
public class LecturerController {

    private final LecturerService lecturerService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public LecturerController(LecturerService lecturerService,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder) {
        this.lecturerService = lecturerService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/courses")
    public List<CourseResponseDTO> getCourses(@RequestParam long lecturerId) {
        return lecturerService.getCoursesByLecturerId(lecturerId);
    }

    @GetMapping("/courses/{offeringId}/schedule")
    public List<ClassSchedule> getClassSchedules(
            @PathVariable long offeringId, 
            @RequestParam long lecturerId) {
        return lecturerService.getClassSchedulesByLecturerId(offeringId, lecturerId);
    }

    @GetMapping("/courses/{offeringId}/students")
    public List<User> getEnrolledStudents(
            @PathVariable long offeringId, 
            @RequestParam long lecturerId) {
        return lecturerService.getEnrolledStudents(offeringId, lecturerId);
    }

    @PostMapping("/attendance")
    public ResponseEntity<String> recordAttendance(
            @RequestBody AttendanceRequestDTO attendanceRequestDTO,
            @RequestParam long studentId,
            @RequestParam String status,
            @RequestParam long lecturerId) {

        lecturerService.recordAttendance(attendanceRequestDTO, studentId, status, lecturerId);

        return ResponseEntity.ok("Attendance recorded successfully.");
    }

    @GetMapping("/attendance/{scheduleId}")
    public List<Attendance> getAttendanceRecords(
            @PathVariable long scheduleId,
            @RequestParam long lecturerId) {
        return lecturerService.getAttendanceRecords(scheduleId, lecturerId);
    }

    @PostMapping("/courses/{offeringId}/code/regenerate")
    public ResponseEntity<String> regenerateCode(
            @PathVariable Long offeringId,
            @RequestBody Map<String, String> payload,
            @RequestParam long lecturerId) {

        String password = payload.get("password");
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecturer not found"));

        if (!passwordEncoder.matches(password, lecturer.getPassword())) {
            throw new UnauthorizedException("Invalid password");
        }

        String newCode = lecturerService.regenerateOfferingCode(offeringId);

        return ResponseEntity.ok(newCode);
    }

}
