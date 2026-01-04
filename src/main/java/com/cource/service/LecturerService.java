package com.cource.service;

import java.util.List;
import java.util.Map;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.course.CourseOfferingRequestDTO;
import com.cource.dto.course.CourseResponseDTO;
import com.cource.dto.lecturer.LecturerDashboardDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.CourseOffering;
import com.cource.entity.Student;

public interface LecturerService {
    // Dashboard & Stats
    LecturerDashboardDTO getDashboardStats(Long lecturerId);
    Map<String, Long> getAttendanceCountsByDate(long lecturerId, int days);
    Map<String, Double> getCourseAverageGradeByLecturer(long lecturerId);

    // Courses & Offerings
    List<CourseResponseDTO> getCoursesByLecturerId(long lecturerId);
    List<CourseOffering> getOfferingsByLecturerId(long lecturerId);
    CourseOffering getOfferingById(long lecturerId, long offeringId);
    CourseOffering createCourseOffering(long lecturerId, CourseOfferingRequestDTO dto);
    CourseOffering updateCourseOffering(long lecturerId, long offeringId, CourseOfferingRequestDTO dto);
    void deleteCourseOffering(long lecturerId, long offeringId);

    // Code Generation
    String regenerateOfferingCode(Long offeringId);
    // This overload is required by your LecturerController
    CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId);

    // Schedules & Students
    List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId);
    List<Student> getEnrolledStudents(long offeringId, long lecturerId);

    // Attendance
    void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status);
    void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status, long lecturerId);
    Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId);
    void deleteAttendance(long attendanceId, Long lecturerId);
    List<Attendance> getAttendanceRecords(long scheduleId, Long lecturerId);

    // This is required by LecturerController
    List<Map<String, Object>> getAttendanceRecordsAsDto(long scheduleId, Long lecturerId);
}