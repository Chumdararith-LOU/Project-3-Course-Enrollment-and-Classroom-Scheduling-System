package com.cource.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@Table(name = "course_offerings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "course_id", "term_id" })
})
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({ "offerings", "prerequisites", "dependentCourses" })
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "term_id", nullable = false)
    @JsonIgnoreProperties({ "offerings" })
    private AcademicTerm term;

    @Column(nullable = false)
    private int capacity = 30;

    @Column(name = "is_active")
    private boolean active = true;

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "offering" })
    private List<CourseLecturer> lecturers = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "enrollment_code", nullable = false, unique = true, length = 16)
    private String enrollmentCode;

    @Column(name = "enrollment_code_expires_at")
    private LocalDateTime enrollmentCodeExpiresAt;

    public CourseOffering() {
    }

    public LocalDateTime getEnrollmentCodeExpiresAt() {
        return enrollmentCodeExpiresAt;
    }

    public void setEnrollmentCodeExpiresAt(LocalDateTime enrollmentCodeExpiresAt) {
        this.enrollmentCodeExpiresAt = enrollmentCodeExpiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public AcademicTerm getTerm() {
        return term;
    }

    public void setTerm(AcademicTerm term) {
        this.term = term;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<CourseLecturer> getLecturers() {
        return lecturers;
    }

    public void setLecturers(List<CourseLecturer> lecturers) {
        this.lecturers = lecturers;
    }

    public String getEnrollmentCode() {
        return enrollmentCode;
    }

    public void setEnrollmentCode(String enrollmentCode) {
        this.enrollmentCode = enrollmentCode;
    }

}