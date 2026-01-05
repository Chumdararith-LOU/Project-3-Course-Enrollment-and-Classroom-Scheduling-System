package com.cource.service;

import java.util.List;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.User;

public interface LecturerService {
        List<Course> getCoursesByLecturerId(long lecturerId);

        List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId);

        List<User> getEnrolledStudents(long offeringId, long lecturerId);

        void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status);

        // Update an existing attendance record (only allowed for lecturer who owns the
        // offering)
        com.cource.entity.Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId);

        // Delete an attendance record (only allowed for lecturer who owns the offering)
        void deleteAttendance(long attendanceId, Long lecturerId);

        List<Attendance> getAttendanceRecords(long scheduleId, Long lecturerId);

        java.util.List<java.util.Map<String, Object>> getAttendanceRecordsAsDto(long scheduleId, Long lecturerId);

        // Attendance trends for a lecturer (counts per date)
        java.util.Map<String, Long> getAttendanceCountsByDate(long lecturerId, int days);

        // Attendance trends (date range + optional course + optional enrollment status)
        java.util.Map<String, Long> getAttendanceCountsByDateRange(long lecturerId,
                        java.time.LocalDate from,
                        java.time.LocalDate to,
                        Long offeringId,
                        String studentStatus);

        // Reports
        double calculatePassRate(long lecturerId, long offeringId, String studentStatus);

        double calculateAverageAttendance(long lecturerId,
                        java.time.LocalDate from,
                        java.time.LocalDate to,
                        Long offeringId,
                        String studentStatus);

        java.util.List<com.cource.dto.lecturer.LecturerCourseReportDTO> getCourseReports(long lecturerId,
                        java.time.LocalDate from,
                        java.time.LocalDate to,
                        String studentStatus);

        com.cource.dto.lecturer.LecturerCourseDetailDTO getDetailedCourseReport(long lecturerId,
                        long offeringId,
                        java.time.LocalDate from,
                        java.time.LocalDate to,
                        String studentStatus);

        // Course performance (average numeric grade) for courses taught by lecturer
        java.util.Map<String, Double> getCourseAverageGradeByLecturer(long lecturerId);

        // Course offering CRUD
        com.cource.entity.CourseOffering createCourseOffering(long lecturerId,
                        com.cource.dto.course.CourseOfferingRequestDTO dto);

        com.cource.entity.CourseOffering updateCourseOffering(long lecturerId, long offeringId,
                        com.cource.dto.course.CourseOfferingRequestDTO dto);

        void deleteCourseOffering(long lecturerId, long offeringId);

        java.util.List<com.cource.entity.CourseOffering> getOfferingsByLecturerId(long lecturerId);

        com.cource.entity.CourseOffering getOfferingById(long lecturerId, long offeringId);

        com.cource.entity.CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId);

}
