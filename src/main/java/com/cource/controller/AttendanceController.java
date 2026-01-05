package com.cource.controller;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.attendance.AttendanceResponseDTO;
import com.cource.dto.attendance.AttendanceSummaryDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.exception.ResourceNotFoundException;
import com.cource.service.AttendanceService;
import com.cource.util.SecurityHelper;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Collections;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SecurityHelper securityHelper;

    /**
     * Record attendance for a single student
     */
    @PostMapping
    public ResponseEntity<?> recordAttendance(@RequestBody AttendanceRequestDTO request) {
        try {
            // Set the current user as recorder if not specified
            if (request.getLecturerId() == null) {
                request.setLecturerId(securityHelper.getCurrentUserId());
            }
            Attendance attendance = attendanceService.recordAttendance(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Attendance recorded successfully",
                    "attendanceId", attendance.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Bulk record attendance for multiple students
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkRecordAttendance(
            @RequestParam Long scheduleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam List<Long> studentIds,
            @RequestParam(defaultValue = "PRESENT") String status) {
        try {
            Long recorderId = securityHelper.getCurrentUserId();
            List<Attendance> records = attendanceService.bulkRecordAttendance(
                    scheduleId, date, studentIds, status, recorderId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bulk attendance recorded",
                    "count", records.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance records by schedule
     */
    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<List<AttendanceResponseDTO>> getBySchedule(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(attendanceService.getAttendanceBySchedule(scheduleId));
    }

    /**
     * Get attendance records by schedule and date
     */
    @GetMapping("/schedule/{scheduleId}/date/{date}")
    public ResponseEntity<List<AttendanceResponseDTO>> getByScheduleAndDate(
            @PathVariable Long scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceByScheduleAndDate(scheduleId, date));
    }

    /**
     * Get student attendance for an offering
     */
    @GetMapping("/student/{studentId}/offering/{offeringId}")
    public ResponseEntity<List<AttendanceResponseDTO>> getStudentAttendance(
            @PathVariable Long studentId,
            @PathVariable Long offeringId) {
        return ResponseEntity.ok(attendanceService.getStudentAttendance(studentId, offeringId));
    }

    /**
     * Get student attendance summary for an offering
     */
    @GetMapping("/student/{studentId}/offering/{offeringId}/summary")
    public ResponseEntity<AttendanceSummaryDTO> getStudentSummary(
            @PathVariable Long studentId,
            @PathVariable Long offeringId) {
        try {
            return ResponseEntity.ok(attendanceService.getStudentAttendanceSummary(studentId, offeringId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get attendance statistics for a schedule
     */
    @GetMapping("/schedule/{scheduleId}/stats")
    public ResponseEntity<Map<String, Object>> getScheduleStats(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(attendanceService.getScheduleAttendanceStats(scheduleId));
    }

    /**
     * Get attendance rate for a student in an offering
     */
    @GetMapping("/student/{studentId}/offering/{offeringId}/rate")
    public ResponseEntity<Map<String, Object>> getAttendanceRate(
            @PathVariable Long studentId,
            @PathVariable Long offeringId) {
        double rate = attendanceService.getAttendanceRate(studentId, offeringId);
        return ResponseEntity.ok(Map.of("studentId", studentId, "offeringId", offeringId, "rate", rate));
    }

    /**
     * Get today's schedules for current lecturer
     */
    @GetMapping("/today")
    public ResponseEntity<List<ClassSchedule>> getTodaySchedules(@RequestParam(required = false) Long lecturerId) {
        Long id = lecturerId != null ? lecturerId : securityHelper.getCurrentUserId();
        if (id == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(attendanceService.getTodaySchedulesForLecturer(id));
    }

    /**
     * Check if attendance exists
     */
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> checkExists(
            @RequestParam Long studentId,
            @RequestParam Long scheduleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean exists = attendanceService.attendanceExists(studentId, scheduleId, date);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Update attendance record
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAttendance(
            @PathVariable Long id,
            @RequestBody AttendanceRequestDTO request) {
        try {
            Attendance updated = attendanceService.updateAttendance(id, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Attendance updated",
                    "id", updated.getId(),
                    "status", updated.getStatus()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete attendance record
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable Long id) {
        try {
            attendanceService.deleteAttendance(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Attendance deleted"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get offering attendance with date range
     */
    @GetMapping("/offering/{offeringId}")
    public ResponseEntity<List<AttendanceResponseDTO>> getOfferingAttendance(
            @PathVariable Long offeringId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.getOfferingAttendance(offeringId, from, to));
    }
}
