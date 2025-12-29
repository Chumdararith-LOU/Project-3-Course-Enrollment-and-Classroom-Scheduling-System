package com.cource.dto.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentDTO {
    private Long enrollmentId;
    private Long offeringId;
    private String courseCode;
    private String title;
    private int credits;
    private String termName;
    private String status;
    private String grade;
    private String lecturer;
    private String schedule;
    private String location;
    private LocalDateTime enrolledAt;
}
