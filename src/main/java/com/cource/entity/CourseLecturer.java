package com.cource.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "course_lecturers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"offering_id", "lecturer_id"})
})
public class CourseLecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    @Column(name = "is_primary")
    private boolean primary = true;

    public CourseLecturer() {}

    public CourseLecturer(CourseOffering offering, User lecturer, boolean primary) {
        this.offering = offering;
        this.lecturer = lecturer;
        this.primary = primary;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CourseOffering getOffering() { return offering; }
    public void setOffering(CourseOffering offering) { this.offering = offering; }
    public User getLecturer() { return lecturer; }
    public void setLecturer(User lecturer) { this.lecturer = lecturer; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
}