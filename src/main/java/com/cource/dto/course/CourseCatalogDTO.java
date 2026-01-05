package com.cource.dto.course;

import lombok.Data;

@Data
public class CourseCatalogDTO {
    private Long id;
    private String code;
    private String title;
    private String description;
    private String lecturer;
    private String level;
    private int credits;
    private int capacity;
    private int enrolled;
    private boolean enrolledStatus;
    private String schedule;
    private String location;
}
