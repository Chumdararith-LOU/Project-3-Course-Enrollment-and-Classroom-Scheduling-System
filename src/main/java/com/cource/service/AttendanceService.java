package com.cource.service;

import com.cource.dto.attendance.AttendanceRequestDTO;
import com.cource.dto.attendance.AttendanceResponseDTO;

import java.util.List;

public interface AttendanceService {
    AttendanceResponseDTO markAttendance(AttendanceRequestDTO dto, String lecturerEmail);
    List<AttendanceResponseDTO> getAttendanceBySchedule(Long scheduleId, String lecturerEmail);
}
