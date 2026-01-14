package com.cource.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.cource.service.LecturerService;
import com.cource.util.SecurityHelper;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class LecturerAttendanceController {

    private final LecturerService lecturerService;
    private final SecurityHelper securityHelper;

    @GetMapping("/api/lecturer/attendance")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAttendanceJson(@RequestParam Long scheduleId) {
        Long lecturerId = securityHelper.getCurrentUserId();
        return ResponseEntity.ok(lecturerService.getAttendanceRecordsAsDto(scheduleId, lecturerId));
    }

    @PostMapping("/api/lecturer/attendance/{id}/action")
    @ResponseBody
    public ResponseEntity<?> actionOnAttendance(@PathVariable Long id, @RequestParam String action,
            @RequestParam(required = false) String status) {
        Long lecturerId = securityHelper.getCurrentUserId();
        try {
            if ("approve".equalsIgnoreCase(action)) {
                com.cource.dto.attendance.AttendanceRequestDTO dto = new com.cource.dto.attendance.AttendanceRequestDTO();
                if (status == null || status.isBlank())
                    dto.setStatus("PRESENT");
                else
                    dto.setStatus(status);
                var updated = lecturerService.updateAttendance(id, dto, lecturerId);
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", updated.getId());
                m.put("attendanceDate", updated.getAttendanceDate());
                m.put("status", updated.getStatus());
                m.put("notes", updated.getNotes());
                if (updated.getEnrollment() != null && updated.getEnrollment().getStudent() != null) {
                    var student = updated.getEnrollment().getStudent();
                    java.util.Map<String, Object> s = new java.util.LinkedHashMap<>();
                    s.put("id", student.getId());
                    s.put("firstName", student.getFirstName());
                    s.put("lastName", student.getLastName());
                    s.put("email", student.getEmail());
                    s.put("fullName", student.getFullName());
                    m.put("student", s);
                }
                if (updated.getRecordedBy() != null) {
                    var rb = updated.getRecordedBy();
                    java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
                    r.put("id", rb.getId());
                    r.put("fullName", rb.getFullName());
                    m.put("recordedBy", r);
                }
                if (updated.getRecordedAt() != null) {
                    m.put("recordedAt", updated.getRecordedAt().toString());
                }
                return ResponseEntity.ok(m);
            } else if ("reject".equalsIgnoreCase(action)) {
                lecturerService.deleteAttendance(id, lecturerId);
                return ResponseEntity.ok(Map.of("deleted", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown action"));
            }
        } catch (Exception ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }
    }

}
