package com.cource.controller;

import com.cource.entity.CourseOffering;
import com.cource.entity.Student;
import com.cource.entity.User;
import com.cource.service.LecturerService;
import com.cource.service.AdminService;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.EnrollmentRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/lecturer")
public class LecturerExportController {
    private final LecturerService lecturerService;
    private final AdminService adminService;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;

    public LecturerExportController(LecturerService lecturerService, AdminService adminService,
            AttendanceRepository attendanceRepository,
            EnrollmentRepository enrollmentRepository) {
        this.lecturerService = lecturerService;
        this.adminService = adminService;
        this.attendanceRepository = attendanceRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping("/courses/export")
    public void exportCourses(@RequestParam Long lecturerId, HttpServletResponse response) throws IOException {
        List<CourseOffering> offerings = lecturerService.getOfferingsByLecturerId(lecturerId);
        String filename = "courses_lecturer_" + lecturerId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        var writer = response.getWriter();
        writer.println("Offering ID,Course Code,Course Title,Term,Capacity,Active");
        for (CourseOffering off : offerings) {
            writer.printf("%d,%s,%s,%s,%d,%s\n",
                    off.getId(),
                    off.getCourse().getCourseCode(),
                    off.getCourse().getTitle().replaceAll(",", " "),
                    off.getTerm().getTermName(),
                    off.getCapacity(),
                    off.isActive() ? "Active" : "Inactive");
        }
        writer.flush();
    }

    @GetMapping("/students/export")
    public void exportStudents(@RequestParam Long offeringId, @RequestParam Long lecturerId,
                               HttpServletResponse response) throws IOException {
        List<Student> students = lecturerService.getEnrolledStudents(offeringId, lecturerId);
        String filename = "students_offering_" + offeringId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");
        var writer = response.getWriter();
        writer.println("Student ID,First Name,Last Name,Email,Active");
        for (Student s : students) {
            writer.printf("%d,%s,%s,%s,%s\n",
                    s.getId(),
                    s.getFirstName() != null ? s.getFirstName().replaceAll(",", " ") : "",
                    s.getLastName() != null ? s.getLastName().replaceAll(",", " ") : "",
                    s.getEmail() != null ? s.getEmail() : "",
                    s.getIsActive() != null && s.getIsActive() ? "Active" : "Inactive");
        }
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
        // verifies ownership
        lecturerService.getOfferingById(lecturerId, offeringId);

        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = (from == null || from.isBlank()) ? LocalDate.now().minusDays(29) : LocalDate.parse(from);
        } catch (Exception ex) {
            fromDate = LocalDate.now().minusDays(29);
        }
        try {
            toDate = (to == null || to.isBlank()) ? LocalDate.now() : LocalDate.parse(to);
        } catch (Exception ex) {
            toDate = LocalDate.now();
        }
        if (toDate.isBefore(fromDate)) {
            var tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        String filename = "attendance_offering_" + offeringId + "_" + fromDate + "_to_" + toDate + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        var rows = attendanceRepository.findByOfferingIdBetweenDates(offeringId, fromDate, toDate);
        var writer = response.getWriter();
        writer.println(
                "Date,Schedule ID,Student ID,Student Name,Student Email,Enrollment Status,Attendance Status,Notes");
        for (var a : rows) {
            var enrol = a.getEnrollment();
            var student = enrol != null ? enrol.getStudent() : null;
            String enrollStatus = enrol != null ? enrol.getStatus() : "";
            if (studentStatus != null && !studentStatus.isBlank()) {
                if (enrollStatus == null || !enrollStatus.equalsIgnoreCase(studentStatus)) {
                    continue;
                }
            }
            String studentName = student != null ? (student.getFirstName() + " " + student.getLastName()).trim() : "";
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s\n",
                    a.getAttendanceDate() != null ? a.getAttendanceDate().toString() : "",
                    a.getSchedule() != null ? a.getSchedule().getId() : "",
                    student != null ? student.getId() : "",
                    studentName.replaceAll(",", " "),
                    student != null && student.getEmail() != null ? student.getEmail() : "",
                    enrollStatus != null ? enrollStatus : "",
                    a.getStatus() != null ? a.getStatus() : "",
                    a.getNotes() != null ? a.getNotes().replaceAll(",", " ") : "");
        }
        writer.flush();
    }

    @GetMapping("/reports/export/grades.csv")
    public void exportGradesCsv(
            @RequestParam Long offeringId,
            @RequestParam Long lecturerId,
            @RequestParam(required = false) String studentStatus,
            HttpServletResponse response) throws IOException {
        // verifies ownership
        lecturerService.getOfferingById(lecturerId, offeringId);

        String filename = "grades_offering_" + offeringId + ".csv";
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        var enrollments = enrollmentRepository.findByOfferingIdWithStudentFiltered(offeringId, studentStatus);
        var writer = response.getWriter();
        writer.println("Student ID,Student Name,Email,Enrollment Status,Grade");
        for (var e : enrollments) {
            var s = e.getStudent();
            String studentName = s != null ? (s.getFirstName() + " " + s.getLastName()).trim() : "";
            writer.printf("%s,%s,%s,%s,%s\n",
                    s != null ? s.getId() : "",
                    studentName.replaceAll(",", " "),
                    s != null && s.getEmail() != null ? s.getEmail() : "",
                    e.getStatus() != null ? e.getStatus() : "",
                    e.getGrade() != null ? e.getGrade() : "");
        }
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
        LocalDate fromDate;
        LocalDate toDate;
        try {
            fromDate = (from == null || from.isBlank()) ? LocalDate.now().minusDays(29) : LocalDate.parse(from);
        } catch (Exception ex) {
            fromDate = LocalDate.now().minusDays(29);
        }
        try {
            toDate = (to == null || to.isBlank()) ? LocalDate.now() : LocalDate.parse(to);
        } catch (Exception ex) {
            toDate = LocalDate.now();
        }
        if (toDate.isBefore(fromDate)) {
            var tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        String filename = "lecturer_report_" + lecturerId + ".pdf";
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        var avgAttendance = lecturerService.calculateAverageAttendance(lecturerId, fromDate, toDate, offeringId,
                studentStatus);
        double passRate = 0.0;
        if (offeringId != null) {
            passRate = lecturerService.calculatePassRate(lecturerId, offeringId, studentStatus);
        }

        var courseReports = lecturerService.getCourseReports(lecturerId, fromDate, toDate, studentStatus);

        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
            doc.addPage(page);

            try (var cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(50, 740);
                cs.showText("Lecturer Reports & Analytics");

                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(0, -24);
                cs.showText("Lecturer ID: " + lecturerId);
                cs.newLineAtOffset(0, -16);
                cs.showText("Date range: " + fromDate + " to " + toDate);
                cs.newLineAtOffset(0, -16);
                cs.showText("Course offering filter: " + (offeringId != null ? offeringId : "All"));
                cs.newLineAtOffset(0, -16);
                cs.showText("Student status filter: "
                        + (studentStatus != null && !studentStatus.isBlank() ? studentStatus : "All"));

                cs.newLineAtOffset(0, -24);
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
                cs.showText("Summary");
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(0, -16);
                cs.showText(String.format("Average attendance: %.1f%%", avgAttendance));
                cs.newLineAtOffset(0, -16);
                cs.showText(String.format("Pass rate (selected course only): %.1f%%", passRate));

                cs.newLineAtOffset(0, -24);
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 12);
                cs.showText("Course Performance (top 10)");
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 10);

                int printed = 0;
                for (var r : courseReports) {
                    if (printed >= 10)
                        break;
                    cs.newLineAtOffset(0, -14);
                    String line = String.format("%s %s | Students: %d | AvgAttend: %.1f%% | Pass: %.1f%%",
                            r.getCourseCode(),
                            r.getCourseName(),
                            r.getStudentCount(),
                            r.getAvgAttendancePercent(),
                            r.getPassRatePercent());
                    if (line.length() > 110) {
                        line = line.substring(0, 110);
                    }
                    cs.showText(line);
                    printed++;
                }

                cs.endText();
            }

            doc.save(response.getOutputStream());
        }
    }
}
