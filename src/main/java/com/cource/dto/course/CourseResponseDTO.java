package com.cource.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {
    private Long id;
    private String courseCode;
    private String title;
    private String description;
    private int credits;
    private int capacity;

    private boolean active;

    private String lecturer;
    private String schedule;
    private String location;
    private String enrollmentCode;

    private LocalDateTime enrollmentCodeExpiresAt;
    private boolean isCodeExpired;

    private int enrolled;
    private boolean enrolledStatus;
}
