package com.cource.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.course.CourseOfferingRequestDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.CourseOffering;
import com.cource.entity.Enrollment;
import com.cource.entity.User;

public interface LecturerService {
        List<Course> getCoursesByLecturerId(long lecturerId);

        List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId);

        List<User> getEnrolledStudents(long offeringId, long lecturerId);

        void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status);

        Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId);

        // Delete an attendance record (only allowed for lecturer who owns the offering)
        void deleteAttendance(long attendanceId, Long lecturerId);

        List<Attendance> getAttendanceRecords(long scheduleId, Long lecturerId);

        List<Map<String, Object>> getAttendanceRecordsAsDto(long scheduleId, Long lecturerId);

        // Attendance trends for a lecturer (counts per date)
        Map<String, Long> getAttendanceCountsByDate(long lecturerId, int days);

        // Attendance trends (date range + optional course + optional enrollment status)
        Map<String, Long> getAttendanceCountsByDateRange(long lecturerId,
                        LocalDate from,
                        LocalDate to,
                        Long offeringId,
                        String studentStatus);

        // Reports
        double calculatePassRate(long lecturerId, long offeringId, String studentStatus);

        double calculateAverageAttendance(long lecturerId,
                        LocalDate from,
                        LocalDate to,
                        Long offeringId,
                        String studentStatus);

        List<com.cource.dto.lecturer.LecturerCourseReportDTO> getCourseReports(long lecturerId,
                        LocalDate from,
                        LocalDate to,
                        String studentStatus);

        com.cource.dto.lecturer.LecturerCourseDetailDTO getDetailedCourseReport(long lecturerId,
                        long offeringId,
                        LocalDate from,
                        LocalDate to,
                        String studentStatus);

        // Course performance (average numeric grade) for courses taught by lecturer
        Map<String, Double> getCourseAverageGradeByLecturer(long lecturerId);

        // Enrollment grade update (only allowed for lecturer who owns the offering)
        Enrollment updateEnrollmentGrade(long lecturerId, long enrollmentId, String grade);

        // Course offering CRUD
        CourseOffering createCourseOffering(long lecturerId, CourseOfferingRequestDTO dto);

        CourseOffering updateCourseOffering(long lecturerId, long offeringId, CourseOfferingRequestDTO dto);

        void deleteCourseOffering(long lecturerId, long offeringId);

        List<CourseOffering> getOfferingsByLecturerId(long lecturerId);

        CourseOffering getOfferingById(long lecturerId, long offeringId);

        CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId);

}
