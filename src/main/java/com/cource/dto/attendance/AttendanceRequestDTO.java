package com.cource.dto.attendance;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDTO {

    private Long studentId;
    private Long scheduleId;
    private Long enrollmentId;
    private Long lecturerId;
    private String status;
    private LocalDate attendanceDate;
    private String notes;
}
