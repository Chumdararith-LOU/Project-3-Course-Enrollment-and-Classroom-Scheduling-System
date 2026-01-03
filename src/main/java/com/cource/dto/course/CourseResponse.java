package com.cource.dto.course;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LecturerInfo {
        private Long id;
        private String name;
        private String email;
    }
}
