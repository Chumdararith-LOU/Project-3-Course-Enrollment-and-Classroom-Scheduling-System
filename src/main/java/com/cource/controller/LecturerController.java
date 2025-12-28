package com.cource.controller;

import com.cource.dto.attendance.AttendanceRequestDTO;
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
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping("/courses")
    public List<Course> getCourses(@RequestParam long lecturerId) {
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

}
