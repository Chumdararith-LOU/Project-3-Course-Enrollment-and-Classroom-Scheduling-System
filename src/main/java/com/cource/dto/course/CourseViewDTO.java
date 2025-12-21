package com.cource.dto.course;

public class CourseViewDTO {
    public String code;
    public String level;
    public String title;
    public String lecturer;
    public String description;
    public int enrolled;
    public int capacity;
    public int credits;

    public CourseViewDTO(String code, String level, String title,
                          String lecturer, String description,
                          int enrolled, int capacity, int credits) {
        this.code = code;
        this.level = level;
        this.title = title;
        this.lecturer = lecturer;
        this.description = description;
        this.enrolled = enrolled;
        this.capacity = capacity;
        this.credits = credits;
    }
}
