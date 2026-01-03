package com.cource.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LecturerCourseDTO {
    private Long offeringId;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private String description;
    private Integer credits;
    private Boolean isActive;
    private String termCode;
    private String termName;
    private Integer capacity;
    private Long enrolledCount;
    private Boolean isPrimaryLecturer;
}
