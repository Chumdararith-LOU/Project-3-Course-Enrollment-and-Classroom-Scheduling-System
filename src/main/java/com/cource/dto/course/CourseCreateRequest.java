package com.cource.dto.course;

import java.util.List;

import lombok.Data;

@Data
public class CourseCreateRequest {
    private String courseCode;
    private String title;
    private String description;
    private Integer credits;
    private Integer capacity;
    private Boolean active = true;

    private List<Long> lectureIds;
}
