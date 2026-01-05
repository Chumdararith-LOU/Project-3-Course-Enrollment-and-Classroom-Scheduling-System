package com.cource.dto.lecturer;

import java.util.List;
import java.util.Map;

public class LecturerCourseDetailDTO {
    private LecturerCourseReportDTO summary;
    private Map<String, Long> gradeDistribution;
    private List<Map<String, Object>> studentGrades;

    public LecturerCourseDetailDTO() {
    }

    public LecturerCourseDetailDTO(LecturerCourseReportDTO summary, Map<String, Long> gradeDistribution,
            List<Map<String, Object>> studentGrades) {
        this.summary = summary;
        this.gradeDistribution = gradeDistribution;
        this.studentGrades = studentGrades;
    }

    public LecturerCourseReportDTO getSummary() {
        return summary;
    }

    public void setSummary(LecturerCourseReportDTO summary) {
        this.summary = summary;
    }

    public Map<String, Long> getGradeDistribution() {
        return gradeDistribution;
    }

    public void setGradeDistribution(Map<String, Long> gradeDistribution) {
        this.gradeDistribution = gradeDistribution;
    }

    public List<Map<String, Object>> getStudentGrades() {
        return studentGrades;
    }

    public void setStudentGrades(List<Map<String, Object>> studentGrades) {
        this.studentGrades = studentGrades;
    }
}
