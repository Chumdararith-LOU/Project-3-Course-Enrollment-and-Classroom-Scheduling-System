package com.cource.dto.attendance;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDTO {

    private Long studentId;
    private Long scheduleId;
    private String status; // "PRESENT", "ABSENT", "LATE", "EXCUSED"
    private LocalDate attendanceDate;
    private String notes;
}
