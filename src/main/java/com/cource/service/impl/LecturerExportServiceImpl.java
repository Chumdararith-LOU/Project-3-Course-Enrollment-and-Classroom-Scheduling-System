package com.cource.service.impl;

import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;
import com.cource.repository.AttendanceRepository;
import com.cource.repository.EnrollmentRepository;
import com.cource.service.LecturerExportService;
import com.cource.service.LecturerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LecturerExportServiceImpl implements LecturerExportService {

    private final LecturerService lecturerService;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public String exportAttendanceForScheduleCsv(Long scheduleId, Long currentLecturerId) {
        var rows = lecturerService.getAttendanceRecords(scheduleId, currentLecturerId);

        StringBuilder csv = new StringBuilder();
        csv.append(
                "Date,Schedule ID,Student ID,Student Name,Student Email,Attendance Status,Notes,Recorded By,Recorded At\n");
        for (var a : rows) {
            var enrol = a.getEnrollment();
            var student = enrol != null ? enrol.getStudent() : null;
            String studentName = student != null ? (student.getFirstName() + " " + student.getLastName()).trim() : "";
            String recordedBy = a.getRecordedBy() != null ? a.getRecordedBy().getFullName() : "";
            String recordedAt = a.getRecordedAt() != null ? a.getRecordedAt().toString() : "";

            csv.append(safe(a.getAttendanceDate() != null ? a.getAttendanceDate().toString() : "")).append(',');
            csv.append(safe(
                    a.getSchedule() != null ? String.valueOf(a.getSchedule().getId()) : String.valueOf(scheduleId)))
                    .append(',');
            csv.append(safe(student != null ? String.valueOf(student.getId()) : "")).append(',');
            csv.append(safe(studentName)).append(',');
            csv.append(safe(student != null && student.getEmail() != null ? student.getEmail() : "")).append(',');
            csv.append(safe(a.getStatus() != null ? a.getStatus() : "")).append(',');
            csv.append(safe(a.getNotes() != null ? a.getNotes() : "")).append(',');
            csv.append(safe(recordedBy)).append(',');
            csv.append(safe(recordedAt)).append('\n');
        }

        return csv.toString();
    }

    @Override
    public String exportCoursesCsv(Long currentLecturerId) {
        List<CourseOffering> offerings = lecturerService.getOfferingsByLecturerId(currentLecturerId);

        StringBuilder csv = new StringBuilder();
        csv.append("Offering ID,Course Code,Course Title,Term,Capacity,Active\n");
        for (CourseOffering off : offerings) {
            csv.append(off.getId()).append(',');
            csv.append(safe(off.getCourse().getCourseCode())).append(',');
            csv.append(safe(off.getCourse().getTitle())).append(',');
            csv.append(safe(off.getTerm().getTermName())).append(',');
            csv.append(off.getCapacity()).append(',');
            csv.append(off.isActive() ? "Active" : "Inactive").append('\n');
        }
        return csv.toString();
    }

    @Override
    public String exportStudentsCsv(Long offeringId, Long currentLecturerId) {
        lecturerService.getOfferingById(currentLecturerId, offeringId);

        List<User> students = enrollmentRepository.findByOfferingIdWithStudentFiltered(offeringId, "ENROLLED")
                .stream()
                .map(Enrollment::getStudent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Student ID,First Name,Last Name,Email,Active\n");
        for (User s : students) {
            csv.append(s.getId()).append(',');
            csv.append(safe(s.getFirstName())).append(',');
            csv.append(safe(s.getLastName())).append(',');
            csv.append(safe(s.getEmail())).append(',');
            csv.append(s.isActive() ? "Active" : "Inactive").append('\n');
        }
        return csv.toString();
    }

    @Override
    public String exportAttendanceCsv(Long offeringId, Long currentLecturerId, String from, String to,
            String studentStatus) {
        // verifies ownership
        lecturerService.getOfferingById(currentLecturerId, offeringId);

        LocalDate[] range = resolveDateRange(from, to);
        LocalDate fromDate = range[0];
        LocalDate toDate = range[1];

        var rows = attendanceRepository.findByOfferingIdBetweenDates(offeringId, fromDate, toDate);

        StringBuilder csv = new StringBuilder();
        csv.append(
                "Date,Schedule ID,Student ID,Student Name,Student Email,Enrollment Status,Attendance Status,Notes\n");
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

            csv.append(safe(a.getAttendanceDate() != null ? a.getAttendanceDate().toString() : "")).append(',');
            csv.append(safe(a.getSchedule() != null ? String.valueOf(a.getSchedule().getId()) : "")).append(',');
            csv.append(safe(student != null ? String.valueOf(student.getId()) : "")).append(',');
            csv.append(safe(studentName)).append(',');
            csv.append(safe(student != null && student.getEmail() != null ? student.getEmail() : "")).append(',');
            csv.append(safe(enrollStatus != null ? enrollStatus : "")).append(',');
            csv.append(safe(a.getStatus() != null ? a.getStatus() : "")).append(',');
            csv.append(safe(a.getNotes() != null ? a.getNotes() : "")).append('\n');
        }

        return csv.toString();
    }

    @Override
    public String exportGradesCsv(Long offeringId, Long currentLecturerId, String studentStatus) {
        // verifies ownership
        lecturerService.getOfferingById(currentLecturerId, offeringId);

        var enrollments = enrollmentRepository.findByOfferingIdWithStudentFiltered(offeringId, studentStatus);

        StringBuilder csv = new StringBuilder();
        csv.append("Student ID,Student Name,Email,Enrollment Status,Grade\n");
        for (var e : enrollments) {
            var s = e.getStudent();
            String studentName = s != null ? (s.getFirstName() + " " + s.getLastName()).trim() : "";

            csv.append(safe(s != null ? String.valueOf(s.getId()) : "")).append(',');
            csv.append(safe(studentName)).append(',');
            csv.append(safe(s != null && s.getEmail() != null ? s.getEmail() : "")).append(',');
            csv.append(safe(e.getStatus() != null ? e.getStatus() : "")).append(',');
            csv.append(safe(e.getGrade() != null ? e.getGrade() : "")).append('\n');
        }

        return csv.toString();
    }

    @Override
    public byte[] exportReportPdf(Long currentLecturerId, Long offeringId, String from, String to,
            String studentStatus) {
        LocalDate[] range = resolveDateRange(from, to);
        LocalDate fromDate = range[0];
        LocalDate toDate = range[1];

        var avgAttendance = lecturerService.calculateAverageAttendance(currentLecturerId, fromDate, toDate, offeringId,
                studentStatus);
        double passRate = 0.0;
        if (offeringId != null) {
            passRate = lecturerService.calculatePassRate(currentLecturerId, offeringId, studentStatus);
        }

        var courseReports = lecturerService.getCourseReports(currentLecturerId, fromDate, toDate, studentStatus);

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
                cs.showText("Lecturer ID: " + currentLecturerId);
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
                    if (printed >= 10) {
                        break;
                    }
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

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate report PDF: " + ex.getMessage(), ex);
        }
    }

    @Override
    public LocalDate[] resolveDateRange(String from, String to) {
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

        return new LocalDate[] { fromDate, toDate };
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        // simple CSV safety: remove newlines and replace commas to keep the CSV
        // parseable
        String cleaned = value.replace("\n", " ").replace("\r", " ");
        return cleaned.replace(",", " ").trim();
    }
}
