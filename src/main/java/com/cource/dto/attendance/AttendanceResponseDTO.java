package com.cource.dto.attendance;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceResponseDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentIdCard;
    private String status;
    private LocalDate date;
    private String notes;
}
