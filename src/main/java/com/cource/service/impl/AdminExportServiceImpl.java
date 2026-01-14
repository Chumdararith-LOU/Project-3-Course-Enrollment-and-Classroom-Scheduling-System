package com.cource.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.cource.repository.AttendanceRepository;
import com.cource.service.AdminExportService;
import com.cource.service.AdminService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminExportServiceImpl implements AdminExportService {

    private final AdminService adminService;
    private final AttendanceRepository attendanceRepository;

    @Override
    public String exportUsersCsv() {
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

        return csv.toString();
    }

    @Override
    public String exportCoursesCsv() {
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

        return csv.toString();
    }

    @Override
    public String exportEnrollmentsCsv() {
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

        return csv.toString();
    }

    @Override
    public String exportSchedulesCsv() {
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

        return csv.toString();
    }

    @Override
    public String exportAttendanceCsv(Long offeringId, String from, String to) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        var attendances = attendanceRepository.findByOfferingIdBetweenDates(offeringId, fromDate, toDate);

        StringBuilder csv = new StringBuilder();
        csv.append(
                "attendanceId,offeringId,scheduleId,attendanceDate,status,notes,studentId,studentFullName,recordedById,recordedByFullName,recordedAt\n");

        attendances.forEach(a -> {
            var e = a.getEnrollment();
            var s = e.getStudent();
            var rb = a.getRecordedBy();
            csv.append(a.getId()).append(",");
            csv.append(e.getOffering().getId()).append(",");
            csv.append(a.getSchedule().getId()).append(",");
            csv.append(a.getAttendanceDate()).append(",");
            csv.append("\"").append(a.getStatus()).append("\",");
            csv.append("\"").append(a.getNotes() != null ? a.getNotes().replace("\"", "\"\"") : "").append("\",");
            csv.append(s.getId()).append(",");
            csv.append("\"").append(s.getFirstName()).append(" ").append(s.getLastName()).append("\",");
            if (rb != null) {
                csv.append(rb.getId()).append(",");
                csv.append("\"").append(rb.getFirstName()).append(" ").append(rb.getLastName()).append("\",");
            } else {
                csv.append(",");
                csv.append("\"\"").append(",");
            }
            csv.append(a.getRecordedAt() != null ? a.getRecordedAt() : "").append("\n");
        });

        return csv.toString();
    }
}
