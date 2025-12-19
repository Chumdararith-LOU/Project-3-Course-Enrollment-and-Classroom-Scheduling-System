package com.cource.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.User;
import com.cource.service.LecturerService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/lecturer")
// @PreAuthorize("hasRole('LECTURER')")  // DISABLED FOR TESTING
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping("/courses")
    public List<Course> getCourses(@RequestParam long lecturerId) {
        // TODO: After enabling security, get lecturerId from Authentication
        return lecturerService.getCoursesByLecturerId(lecturerId);
    }

    @GetMapping("/courses/{offeringId}/schedule")
    public List<ClassSchedule> getClassSchedules(
            @PathVariable long offeringId, 
            @RequestParam long lecturerId) {
        // TODO: After enabling security, get lecturerId from Authentication
        return lecturerService.getClassSchedulesByLecturerId(offeringId, lecturerId);
    }

    @GetMapping("/courses/{offeringId}/students")
    public List<User> getEnrolledStudents(
            @PathVariable long offeringId, 
            @RequestParam long lecturerId) {
        // TODO: After enabling security, get lecturerId from Authentication
        return lecturerService.getEnrolledStudents(offeringId, lecturerId);
    }

    @PostMapping("/attendance")
    public ResponseEntity<String> recordAttendance(
            @RequestBody com.cource.dto.attendance.AttendanceRequestDTO attendanceRequestDTO,
            @RequestParam long studentId,
            @RequestParam String status) {
        // TODO: After enabling security, validate lecturerId from Authentication
        lecturerService.recordAttendance(attendanceRequestDTO, studentId, status);
        return ResponseEntity.ok("Attendance recorded successfully.");
    }

    @GetMapping("/attendance/{scheduleId}")
    public List<Attendance> getAttendanceRecords(
            @PathVariable long scheduleId,
            @RequestParam long lecturerId) {
        // TODO: After enabling security, get lecturerId from Authentication
        return lecturerService.getAttendanceRecords(scheduleId, lecturerId);
    }

}
