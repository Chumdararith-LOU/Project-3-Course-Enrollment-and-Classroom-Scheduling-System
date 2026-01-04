package com.cource.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_offerings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "course_id", "term_id" })
})
@Getter
@Setter
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({ "offerings" })
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    @JsonIgnoreProperties({ "courseOfferings" })
    private AcademicTerm term;

    @Column(nullable = false)
    private int capacity = 30;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "enrollment_code", nullable = false, unique = true, length = 16)
    private String enrollmentCode;

    @Column(name = "enrollment_code_expires_at")
    private LocalDateTime enrollmentCodeExpiresAt;

    @OneToMany(mappedBy = "offering", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CourseLecturer> lecturers = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public CourseOffering() {
    }
}