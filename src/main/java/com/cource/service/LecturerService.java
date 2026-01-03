package com.cource.service;

import java.util.List;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.course.CourseOfferingRequestDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.entity.Course;
import com.cource.entity.CourseOffering;
import com.cource.entity.User;

public interface LecturerService {
    List<Course> getCoursesByLecturerId(long lecturerId);

    List<ClassSchedule> getClassSchedulesByLecturerId(long offeringId, long lecturerId);

    List<User> getEnrolledStudents(long offeringId, long lecturerId);

    void recordAttendance(AttendanceRequestDTO attendanceRequestDTO, long studentId, String status);

    // Update an existing attendance record (only allowed for lecturer who owns the
    // offering)
    Attendance updateAttendance(long attendanceId, AttendanceRequestDTO dto, Long lecturerId);

    // Delete an attendance record (only allowed for lecturer who owns the offering)
    void deleteAttendance(long attendanceId, Long lecturerId);

    List<Attendance> getAttendanceRecords(long scheduleId, Long lecturerId);

    java.util.List<java.util.Map<String, Object>> getAttendanceRecordsAsDto(long scheduleId, Long lecturerId);

    // Attendance trends for a lecturer (counts per date)
    java.util.Map<String, Long> getAttendanceCountsByDate(long lecturerId, int days);

    // Course performance (average numeric grade) for courses taught by lecturer
    java.util.Map<String, Double> getCourseAverageGradeByLecturer(long lecturerId);

    // Course offering CRUD
    CourseOffering createCourseOffering(long lecturerId,
            CourseOfferingRequestDTO dto);

    CourseOffering updateCourseOffering(long lecturerId, long offeringId,
            CourseOfferingRequestDTO dto);

    void deleteCourseOffering(long lecturerId, long offeringId);

    java.util.List<CourseOffering> getOfferingsByLecturerId(long lecturerId);

    CourseOffering getOfferingById(long lecturerId, long offeringId);

    CourseOffering regenerateOfferingEnrollmentCode(long lecturerId, long offeringId);

}
