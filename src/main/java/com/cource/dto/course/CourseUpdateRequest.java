package com.cource.dto.course;

import java.util.List;

import lombok.Data;

@Data
public class CourseUpdateRequest {
    private String courseCode;
    private String title;
    private String description;
    private int capacity;
    private boolean active;

    private List<Long> lectureIds;
}
