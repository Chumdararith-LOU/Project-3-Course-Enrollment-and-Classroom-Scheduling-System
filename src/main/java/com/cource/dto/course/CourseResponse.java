package com.cource.dto.course;

import java.util.List;

import lombok.Data;

@Data
public class CourseResponse {
    private Long id;
    private String courseCode;
    private String enrollmentCode;
    private String title;
    private String description;
    private int credits;
    private boolean active;

    private List<LecturerInfo> lecturers;

    @Data
    public static class LecturerInfo {
        private Long id;
        private String fullName;
        private String email;

        public LecturerInfo(Long id, String fullName, String email) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
        }
    }

    public CourseResponse(Long id, String courseCode, String enrollmentCode, String title,
            String description, int credits, boolean active) {
        this.id = id;
        this.courseCode = courseCode;
        this.enrollmentCode = enrollmentCode;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.active = active;
        this.lecturers = new java.util.ArrayList<>();
    }
}
