package com.cource.dto.course;

import java.util.List;

import lombok.Data;

@Data
public class CourseCreateRequest {
    private String courseCode;
    private String title;
    private String description;
    private int credits = 3;
    private int capacity;
    private boolean active = true;

    private List<Long> lectureIds;
}
