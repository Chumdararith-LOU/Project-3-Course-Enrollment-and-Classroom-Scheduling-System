package com.cource.service;

import java.util.Optional;
import com.cource.entity.AttendanceCode;

public interface AttendanceCodeService {
    AttendanceCode generateAttendanceCode(Long scheduleId, Long lecturerId);
    AttendanceCode findByScheduleId(Long scheduleId);
    AttendanceCode findByCode(String code);
    AttendanceCode save(AttendanceCode code);
    boolean isValidCode(String code, Long scheduleId);
    void deleteByScheduleId(Long scheduleId);
}
