package com.cource.controller;

import com.cource.service.LecturerExportService;
import com.cource.util.SecurityHelper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/lecturer")
@PreAuthorize("hasRole('LECTURER')")
public class LecturerExportController {
    private final LecturerExportService lecturerExportService;
    private final SecurityHelper securityHelper;

    public LecturerExportController(LecturerExportService lecturerExportService, SecurityHelper securityHelper) {
        this.lecturerExportService = lecturerExportService;
        this.securityHelper = securityHelper;
    }

    @GetMapping("/attendance/export")
    public void exportAttendanceForSchedule(
            @RequestParam Long scheduleId,
            @RequestParam(required = false) Long lecturerId,
            HttpServletResponse response) throws IOException {
        Long currentLecturerId = securityHelper.getCurrentUserId();
        if (currentLecturerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (lecturerId != null && !lecturerId.equals(currentLecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        String csv = lecturerExportService.exportAttendanceForScheduleCsv(scheduleId, currentLecturerId);

        String filename = "attendance_schedule_" + scheduleId + "_lecturer_" + currentLecturerId + ".csv";
        response.setContentType("text/csv");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        var writer = response.getWriter();
        writer.write(csv);
        writer.flush();
    }

    @GetMapping("/courses/export")
    public void exportCourses(@RequestParam Long lecturerId, HttpServletResponse response) throws IOException {
        Long currentLecturerId = securityHelper.getCurrentUserId();
        if (currentLecturerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (lecturerId != null && !lecturerId.equals(currentLecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        String csv = lecturerExportService.exportCoursesCsv(currentLecturerId);
        String filename = "courses_lecturer_" + lecturerId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        var writer = response.getWriter();
        writer.write(csv);
        writer.flush();
    }

    @GetMapping("/students/export")
    public void exportStudents(@RequestParam Long offeringId, @RequestParam Long lecturerId,
            HttpServletResponse response) throws IOException {
        Long currentLecturerId = securityHelper.getCurrentUserId();
        if (currentLecturerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (lecturerId != null && !lecturerId.equals(currentLecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        String csv = lecturerExportService.exportStudentsCsv(offeringId, currentLecturerId);
        String filename = "students_offering_" + offeringId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        var writer = response.getWriter();
        writer.write(csv);
        writer.flush();
    }

    @GetMapping("/reports/export/attendance.csv")
    public void exportAttendanceCsv(
            @RequestParam Long offeringId,
            @RequestParam Long lecturerId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String studentStatus,
            HttpServletResponse response) throws IOException {
        Long currentLecturerId = securityHelper.getCurrentUserId();
        if (currentLecturerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (lecturerId != null && !lecturerId.equals(currentLecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        LocalDate[] range = lecturerExportService.resolveDateRange(from, to);
        LocalDate fromDate = range[0];
        LocalDate toDate = range[1];

        String csv = lecturerExportService.exportAttendanceCsv(offeringId, currentLecturerId, from, to, studentStatus);

        String filename = "attendance_offering_" + offeringId + "_" + fromDate + "_to_" + toDate + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        var writer = response.getWriter();
        writer.write(csv);
        writer.flush();
    }

    @GetMapping("/reports/export/grades.csv")
    public void exportGradesCsv(
            @RequestParam Long offeringId,
            @RequestParam Long lecturerId,
            @RequestParam(required = false) String studentStatus,
            HttpServletResponse response) throws IOException {
        Long currentLecturerId = securityHelper.getCurrentUserId();
        if (currentLecturerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (lecturerId != null && !lecturerId.equals(currentLecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        String csv = lecturerExportService.exportGradesCsv(offeringId, currentLecturerId, studentStatus);

        String filename = "grades_offering_" + offeringId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        var writer = response.getWriter();
        writer.write(csv);
        writer.flush();
    }

    @GetMapping("/reports/export/report.pdf")
    public void exportReportPdf(
            @RequestParam Long lecturerId,
            @RequestParam(required = false) Long offeringId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String studentStatus,
            HttpServletResponse response) throws IOException {
        Long currentLecturerId = securityHelper.getCurrentUserId();
        if (currentLecturerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        if (lecturerId != null && !lecturerId.equals(currentLecturerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }

        String filename = "lecturer_report_" + lecturerId + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        byte[] pdf = lecturerExportService.exportReportPdf(currentLecturerId, offeringId, from, to, studentStatus);
        response.getOutputStream().write(pdf);
    }
}
