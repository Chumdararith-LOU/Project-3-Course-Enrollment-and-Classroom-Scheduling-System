package com.cource.service;

public interface AdminExportService {
    String exportUsersCsv();

    String exportCoursesCsv();

    String exportEnrollmentsCsv();

    String exportSchedulesCsv();

    String exportAttendanceCsv(Long offeringId, String from, String to);
}
