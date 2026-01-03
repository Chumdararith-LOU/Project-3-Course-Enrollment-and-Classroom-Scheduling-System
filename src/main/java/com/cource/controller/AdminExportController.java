package com.cource.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.cource.service.AdminService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class AdminExportController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<String> exportUsers() {
        var users = adminService.getAllUsers();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Email,First Name,Last Name,ID Card,Role,Is Active\n");

        users.forEach(user -> {
            csv.append(user.getId()).append(",");
            csv.append("\"").append(user.getEmail()).append("\",");
            csv.append("\"").append(user.getFirstName()).append("\",");
            csv.append("\"").append(user.getLastName()).append("\",");
            csv.append("\"").append(user.getIdCard() != null ? user.getIdCard() : "").append("\",");
            csv.append("\"").append(user.getRole().getRoleName()).append("\",");
            csv.append(user.isActive()).append("\n");
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/courses")
    public ResponseEntity<String> exportCourses() {
        var courses = adminService.getAllCourses();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Course Code,Title,Description,Credits,Is Active\n");

        courses.forEach(course -> {
            csv.append(course.getId()).append(",");
            csv.append("\"").append(course.getCourseCode()).append("\",");
            csv.append("\"").append(course.getTitle()).append("\",");
            csv.append("\"")
                    .append(course.getDescription() != null ? course.getDescription().replace("\"", "\"\"") : "")
                    .append("\",");
            csv.append(course.getCredits()).append(",");
            csv.append(course.isActive()).append("\n");
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=courses.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/enrollments")
    public ResponseEntity<String> exportEnrollments() {
        var enrollments = adminService.getAllEnrollments();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Student ID,Student Name,Course Code,Course Title,Term,Status,Grade,Enrolled Date\n");

        enrollments.forEach(enrollment -> {
            csv.append(enrollment.getId()).append(",");
            csv.append(enrollment.getStudent().getId()).append(",");
            csv.append("\"").append(enrollment.getStudent().getFirstName()).append(" ")
                    .append(enrollment.getStudent().getLastName()).append("\",");
            csv.append("\"").append(enrollment.getOffering().getCourse().getCourseCode()).append("\",");
            csv.append("\"").append(enrollment.getOffering().getCourse().getTitle()).append("\",");
            csv.append("\"").append(enrollment.getOffering().getTerm().getTermName()).append("\",");
            csv.append("\"").append(enrollment.getStatus()).append("\",");
            csv.append("\"").append(enrollment.getGrade() != null ? enrollment.getGrade() : "").append("\",");
            csv.append("\"").append("").append("\"\n");
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=enrollments.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    @GetMapping("/schedules")
    public ResponseEntity<String> exportSchedules() {
        var schedules = adminService.getAllSchedules();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Course Code,Course Title,Term,Day,Start Time,End Time,Room,Building\n");

        schedules.forEach(schedule -> {
            csv.append(schedule.getId()).append(",");
            csv.append("\"").append(schedule.getOffering().getCourse().getCourseCode()).append("\",");
            csv.append("\"").append(schedule.getOffering().getCourse().getTitle()).append("\",");
            csv.append("\"").append(schedule.getOffering().getTerm().getTermName()).append("\",");
            csv.append("\"").append(schedule.getDayOfWeek()).append("\",");
            csv.append(schedule.getStartTime()).append(",");
            csv.append(schedule.getEndTime()).append(",");
            csv.append("\"").append(schedule.getRoom().getRoomNumber()).append("\",");
            csv.append("\"").append(schedule.getRoom().getBuilding()).append("\"\n");
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=schedules.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }
}
