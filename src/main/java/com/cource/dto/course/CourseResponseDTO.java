package com.cource.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
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

    private int enrolled;
    private boolean enrolledStatus;
}
