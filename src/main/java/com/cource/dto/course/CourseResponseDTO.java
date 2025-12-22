package com.cource.dto.course;

public class CourseResponseDTO {
    private Long id;
    private String courseCode;
    private String title;
    private String description;
    private int credits;
    private int capacity;

    private boolean active;

    private String lecturer;
    private String schedule;
    private String location;

    private int enrolled;
    private boolean enrolledStatus;

    // Constructors
    public CourseResponseDTO() {}

    public CourseResponseDTO(Long id, String code, String title, String description, int credits) {
        this.id = id;
        this.courseCode = code;
        this.title = title;
        this.description = description;
        this.credits = credits;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String code) {
        this.courseCode = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLecturer() {
        return lecturer;
    }

    public void setLecturer(String lecturer) {
        this.lecturer = lecturer;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getEnrolled() {
        return enrolled;
    }

    public void setEnrolled(int enrolled) {
        this.enrolled = enrolled;
    }

    public boolean isEnrolledStatus() {
        return enrolledStatus;
    }

    public void setEnrolledStatus(boolean enrolledStatus) {
        this.enrolledStatus = enrolledStatus;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
