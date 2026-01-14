package com.cource.service;

import java.time.LocalDate;

public interface LecturerExportService {

    String exportAttendanceForScheduleCsv(Long scheduleId, Long currentLecturerId);

    String exportCoursesCsv(Long currentLecturerId);

    String exportStudentsCsv(Long offeringId, Long currentLecturerId);

    String exportAttendanceCsv(Long offeringId, Long currentLecturerId, String from, String to, String studentStatus);

    String exportGradesCsv(Long offeringId, Long currentLecturerId, String studentStatus);

    byte[] exportReportPdf(Long currentLecturerId, Long offeringId, String from, String to, String studentStatus);

    LocalDate[] resolveDateRange(String from, String to);
}
