package com.cource.dto.lecturer;

public class LecturerCourseReportDTO {
    private Long offeringId;
    private String courseCode;
    private String courseName;
    private String termName;
    private long studentCount;
    private double avgAttendancePercent;
    private double passRatePercent;
    private boolean active;

    public LecturerCourseReportDTO() {
    }

    public LecturerCourseReportDTO(Long offeringId, String courseCode, String courseName, String termName,
            long studentCount, double avgAttendancePercent, double passRatePercent, boolean active) {
        this.offeringId = offeringId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.termName = termName;
        this.studentCount = studentCount;
        this.avgAttendancePercent = avgAttendancePercent;
        this.passRatePercent = passRatePercent;
        this.active = active;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(Long offeringId) {
        this.offeringId = offeringId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTermName() {
        return termName;
    }

    public void setTermName(String termName) {
        this.termName = termName;
    }

    public long getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(long studentCount) {
        this.studentCount = studentCount;
    }

    public double getAvgAttendancePercent() {
        return avgAttendancePercent;
    }

    public void setAvgAttendancePercent(double avgAttendancePercent) {
        this.avgAttendancePercent = avgAttendancePercent;
    }

    public double getPassRatePercent() {
        return passRatePercent;
    }

    public void setPassRatePercent(double passRatePercent) {
        this.passRatePercent = passRatePercent;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
