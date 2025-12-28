package com.cource.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttendanceSummaryDTO {
    private String status; // "PRESENT", "ABSENT"
    private Long count;
    private Double percentage;
}
