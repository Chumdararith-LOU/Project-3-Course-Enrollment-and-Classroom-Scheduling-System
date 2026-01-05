package com.cource.dto.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private Long scheduleId;
    private String scheduleDayOfWeek;
    private String scheduleTime;
    private Long offeringId;
    private String courseName;
    private String courseCode;
    private LocalDate attendanceDate;
    private String status;
    private String notes;
    private Long recordedById;
    private String recordedByName;
    private LocalDateTime recordedAt;
}
