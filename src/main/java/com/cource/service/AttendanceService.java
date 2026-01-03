package com.cource.service;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.attendance.AttendanceResponseDTO;
import com.cource.dto.attendance.AttendanceSummaryDTO;
import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {
    /**
     * Record attendance for a student in a specific schedule
     */
    Attendance recordAttendance(AttendanceRequestDTO request);

    /**
     * Get attendance records for a specific schedule
     */
    List<AttendanceResponseDTO> getAttendanceBySchedule(Long scheduleId);

    /**
     * Get attendance records for a specific schedule on a specific date
     */
    List<AttendanceResponseDTO> getAttendanceByScheduleAndDate(Long scheduleId, LocalDate date);

    /**
     * Get attendance records for a student in a specific offering
     */
    List<AttendanceResponseDTO> getStudentAttendance(Long studentId, Long offeringId);

    /**
     * Get attendance summary for a student in a specific offering
     */
    AttendanceSummaryDTO getStudentAttendanceSummary(Long studentId, Long offeringId);

    /**
     * Get attendance statistics for a schedule
     */
    Map<String, Object> getScheduleAttendanceStats(Long scheduleId);

    /**
     * Update an existing attendance record
     */
    Attendance updateAttendance(Long attendanceId, AttendanceRequestDTO request);

    /**
     * Delete an attendance record
     */
    void deleteAttendance(Long attendanceId);

    /**
     * Mark attendance for multiple students at once
     */
    List<Attendance> bulkRecordAttendance(Long scheduleId, LocalDate date, List<Long> studentIds, String status,
            Long recordedBy);

    /**
     * Get all schedules for today that a lecturer can mark attendance for
     */
    List<ClassSchedule> getTodaySchedulesForLecturer(Long lecturerId);

    /**
     * Check if attendance already exists for a student on a date
     */
    boolean attendanceExists(Long studentId, Long scheduleId, LocalDate date);

    /**
     * Get attendance rate for a student in an offering
     */
    double getAttendanceRate(Long studentId, Long offeringId);

    /**
     * Get all attendance records for an offering within a date range
     */
    List<AttendanceResponseDTO> getOfferingAttendance(Long offeringId, LocalDate fromDate, LocalDate toDate);
}
