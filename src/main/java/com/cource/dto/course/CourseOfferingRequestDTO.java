package com.cource.dto.course;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class CourseOfferingRequestDTO {
    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotNull(message = "Term ID is required")
    private Long termId;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private Boolean active;

    private String enrollmentCode;
}
