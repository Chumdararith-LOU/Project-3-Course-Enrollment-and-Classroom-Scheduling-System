package com.cource.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.course.CourseOfferingRequestDTO;
import com.cource.dto.lecturer.LecturerCourseDetailDTO;
import com.cource.dto.lecturer.LecturerCourseReportDTO;
import com.cource.entity.*;

public interface LecturerService {
    void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status);
    Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId);
    void deleteAttendance(long attendanceId, Long lecturerId);

    List<Attendance> getAttendanceRecords(long scheduleId, Long lecturerId);
    List<Map<String, Object>> getAttendanceRecordsAsDto(long scheduleId, Long lecturerId);

    Map<String, Long> getAttendanceCountsByDate(long lecturerId, int days);
    Map<String, Long> getAttendanceCountsByDateRange(long lecturerId, LocalDate from, LocalDate to, Long offeringId, String studentStatus);

    double calculatePassRate(long lecturerId, long offeringId, String studentStatus);
    double calculateAverageAttendance(long lecturerId, LocalDate from, LocalDate to, Long offeringId, String studentStatus);

    List<LecturerCourseReportDTO> getCourseReports(long lecturerId, LocalDate from, LocalDate to, String studentStatus);
    LecturerCourseDetailDTO getDetailedCourseReport(long lecturerId, long offeringId, LocalDate from, LocalDate to, String studentStatus);
    Map<String, Double> getCourseAverageGradeByLecturer(long lecturerId);
    Enrollment updateEnrollmentGrade(long lecturerId, long enrollmentId, String grade);

    CourseOffering createCourseOffering(long lecturerId, CourseOfferingRequestDTO dto);
    CourseOffering updateCourseOffering(long lecturerId, long offeringId, CourseOfferingRequestDTO dto);

    void deleteCourseOffering(long lecturerId, long offeringId);
    List<CourseOffering> getOfferingsByLecturerId(long lecturerId);

    CourseOffering getOfferingById(long lecturerId, long offeringId);
    CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId);
}
