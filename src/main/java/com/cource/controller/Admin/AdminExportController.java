package com.cource.controller.Admin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.cource.service.AdminExportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminExportController {

        private final AdminExportService adminExportService;

        @GetMapping("/users")
        public ResponseEntity<String> exportUsers() {
                String csv = adminExportService.exportUsersCsv();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(csv);
        }

        @GetMapping("/courses")
        public ResponseEntity<String> exportCourses() {
                String csv = adminExportService.exportCoursesCsv();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=courses.csv")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(csv);
        }

        @GetMapping("/enrollments")
        public ResponseEntity<String> exportEnrollments() {
                String csv = adminExportService.exportEnrollmentsCsv();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enrollments.csv")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(csv);
        }

        @GetMapping("/schedules")
        public ResponseEntity<String> exportSchedules() {
                String csv = adminExportService.exportSchedulesCsv();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=schedules.csv")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(csv);
        }

        @GetMapping("/attendance")
        public ResponseEntity<String> exportAttendance(@RequestParam Long offeringId,
                        @RequestParam String from,
                        @RequestParam String to) {
                String csv = adminExportService.exportAttendanceCsv(offeringId, from, to);
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=attendance_offering_" + offeringId + ".csv")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(csv);
        }
}
