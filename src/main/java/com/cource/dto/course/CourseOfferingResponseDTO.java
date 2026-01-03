package com.cource.dto.course;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CourseOfferingResponseDTO {
    private Long id;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private Long termId;
    private String termCode;
    private String termName;
    private Integer capacity;
    private Long enrolledCount;
    private Boolean isActive;
}
