package com.cource.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryDTO {
    private Long studentId;
    private String studentName;
    private Long offeringId;
    private String courseName;
    private String courseCode;
    private int totalClasses;
    private int presentCount;
    private int lateCount;
    private int absentCount;
    private int excusedCount;
    private double attendanceRate;
    private String attendanceGrade; // A, B, C, D, F based on rate

    public AttendanceSummaryDTO(String status, long count, double percentage) {
        this.attendanceGrade = status;
        this.totalClasses = (int) count;
        this.attendanceRate = percentage;
    }
}
