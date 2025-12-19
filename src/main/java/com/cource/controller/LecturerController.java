package com.cource.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.User;
import com.cource.service.LecturerService;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/lecturer")
@PreAuthorize("hasRole('LECTURER')")
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping("/courses")
    public List<Course> getCourses(Authentication authentication) {
        long lecturerId = ((User) authentication.getPrincipal()).getId();
        return lecturerService.getCoursesByLecturerId(lecturerId);
    }

    @GetMapping("/courses/{courseId}/schedule")
    public List<ClassSchedule> getClassSchedules(@PathVariable long courseId, Authentication authentication) {
        long lecturerId = ((User) authentication.getPrincipal()).getId();
        return lecturerService.getClassSchedulesByLecturerId(courseId, lecturerId);
    }

    @GetMapping("/courses/{courseId}/students")
    public List<User> getEnrolledStudents(@PathVariable long courseId, Authentication authentication) {
        long lecturerId = ((User) authentication.getPrincipal()).getId();
        return lecturerService.getEnrolledStudents(courseId, lecturerId);
    }

    @PostMapping("/attendance")
    public ResponseEntity<String> recordAttendance(@RequestBody Object attendanceRequestDTO,
            @RequestParam long studentId,
            @RequestParam String status,
            Authentication authentication) {

        lecturerService.recordAttendance((com.cource.dto.attendance.AttendanceRequestDTO) attendanceRequestDTO,
                studentId, status);
        return ResponseEntity.ok("Attendance recorded successfully.");

    }

    @GetMapping("/attendance/{scheduleId}")
    public List<Attendance> getAttendanceRecords(@PathVariable long scheduleId,
            Authentication authentication) {
        long lecturerId = ((User) authentication.getPrincipal()).getId();
        return lecturerService.getAttendanceRecords(scheduleId, lecturerId);
    }

}
