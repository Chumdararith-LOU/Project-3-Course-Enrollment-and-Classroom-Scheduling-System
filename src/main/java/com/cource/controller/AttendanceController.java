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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SecurityHelper securityHelper;

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<?> recordAttendance(@RequestBody AttendanceRequestDTO request) {
        try {
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

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
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

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<List<AttendanceResponseDTO>> getBySchedule(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(attendanceService.getAttendanceBySchedule(scheduleId));
    }

    @GetMapping("/schedule/{scheduleId}/date/{date}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<List<AttendanceResponseDTO>> getByScheduleAndDate(
            @PathVariable Long scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceByScheduleAndDate(scheduleId, date));
    }

    @GetMapping("/student/{studentId}/offering/{offeringId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and @securityHelper.getCurrentUserId() == #studentId)")
    public ResponseEntity<List<AttendanceResponseDTO>> getStudentAttendance(
            @PathVariable Long studentId,
            @PathVariable Long offeringId) {
        return ResponseEntity.ok(attendanceService.getStudentAttendance(studentId, offeringId));
    }

    @GetMapping("/student/{studentId}/offering/{offeringId}/summary")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and @securityHelper.getCurrentUserId() == #studentId)")
    public ResponseEntity<AttendanceSummaryDTO> getStudentSummary(
            @PathVariable Long studentId,
            @PathVariable Long offeringId) {
        try {
            return ResponseEntity.ok(attendanceService.getStudentAttendanceSummary(studentId, offeringId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/schedule/{scheduleId}/stats")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<Map<String, Object>> getScheduleStats(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(attendanceService.getScheduleAttendanceStats(scheduleId));
    }

    @GetMapping("/student/{studentId}/offering/{offeringId}/rate")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and @securityHelper.getCurrentUserId() == #studentId)")
    public ResponseEntity<Map<String, Object>> getAttendanceRate(
            @PathVariable Long studentId,
            @PathVariable Long offeringId) {
        double rate = attendanceService.getAttendanceRate(studentId, offeringId);
        return ResponseEntity.ok(Map.of("studentId", studentId, "offeringId", offeringId, "rate", rate));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<List<ClassSchedule>> getTodaySchedules(@RequestParam(required = false) Long lecturerId) {
        Long id = lecturerId != null ? lecturerId : securityHelper.getCurrentUserId();
        if (id == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(attendanceService.getTodaySchedulesForLecturer(id));
    }

    @GetMapping("/exists")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('STUDENT') and @securityHelper.getCurrentUserId() == #studentId)")
    public ResponseEntity<Map<String, Boolean>> checkExists(
            @RequestParam Long studentId,
            @RequestParam Long scheduleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean exists = attendanceService.attendanceExists(studentId, scheduleId, date);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<?> deleteAttendance(@PathVariable Long id) {
        try {
            attendanceService.deleteAttendance(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Attendance deleted"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/offering/{offeringId}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN')")
    public ResponseEntity<List<AttendanceResponseDTO>> getOfferingAttendance(
            @PathVariable Long offeringId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(attendanceService.getOfferingAttendance(offeringId, from, to));
    }
}
