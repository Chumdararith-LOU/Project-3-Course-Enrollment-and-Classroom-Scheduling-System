package com.cource.dto.course;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CourseRequestDTO {
    @NotBlank(message = "Course title is required")
    private String title;

    @NotBlank(message = "Course code is required")
    @Pattern(regexp = "^[A-Z]{2,4}\\d{2,4}[A-Z]{2,4}$",
            message = "Course code must follow format: 2-4 letters, 2-4 numbers, 2-4 letters (e.g., GIC25WAD)")
    private String courseCode;

    @NotBlank(message = "Description is required")
    @Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 6, message = "Credits cannot exceed 6")
    private int credits = 3;

    @NotNull(message = "Please select an academic term")
    private Long termId;

    @Min(value = 1, message = "Capacity must be positive")
    private int capacity = 30;
}
