package com.cource.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequestDTO {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    private Long enrollmentId;
    private Long lecturerId;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PRESENT|ABSENT|LATE|EXCUSED", message = "Status must be PRESENT, ABSENT, LATE, or EXCUSED")
    private String status;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    private String notes;
}
