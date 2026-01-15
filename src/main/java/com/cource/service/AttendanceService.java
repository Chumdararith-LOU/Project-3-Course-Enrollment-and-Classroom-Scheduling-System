package com.cource.service;

import com.cource.entity.Attendance;
import com.cource.entity.ClassSchedule;
import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.attendance.AttendanceResponseDTO;
import com.cource.dto.attendance.AttendanceSummaryDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AttendanceService {

    Attendance recordAttendance(AttendanceRequestDTO request);

    List<AttendanceResponseDTO> getAttendanceBySchedule(Long scheduleId);
    List<AttendanceResponseDTO> getAttendanceByScheduleAndDate(Long scheduleId, LocalDate date);
    List<AttendanceResponseDTO> getStudentAttendance(Long studentId, Long offeringId);

    AttendanceSummaryDTO getStudentAttendanceSummary(Long studentId, Long offeringId);
    Map<String, Object> getScheduleAttendanceStats(Long scheduleId);
    Attendance updateAttendance(Long attendanceId, AttendanceRequestDTO request);
    void deleteAttendance(Long attendanceId);
    List<Attendance> bulkRecordAttendance(Long scheduleId, LocalDate date, List<Long> studentIds, String status,
            Long recordedBy);
    boolean attendanceExists(Long studentId, Long scheduleId, LocalDate date);
    double getAttendanceRate(Long studentId, Long offeringId);
    List<AttendanceResponseDTO> getOfferingAttendance(Long offeringId, LocalDate fromDate, LocalDate toDate);
}