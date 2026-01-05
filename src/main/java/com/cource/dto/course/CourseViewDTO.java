package com.cource.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseViewDTO {
    private String code;
    private String level;
    private String title;
    private String lecturer;
    private String description;
    private int enrolled;
    private int capacity;
    private int credits;
}
