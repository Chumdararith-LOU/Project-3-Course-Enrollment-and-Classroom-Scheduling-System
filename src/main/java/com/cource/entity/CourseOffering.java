package com.cource.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lecturer_id", nullable = false)
    @JsonIgnoreProperties({ "enrollments", "password", "profile" })
    private User lecturer;

    @Column(nullable = false)
    private int capacity = 30;

    @Column(name = "is_active")
    private boolean active = true;

    @OneToMany(mappedBy = "offering", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "offering", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Waitlist> waitlistEntries = new ArrayList<>();

    @OneToMany(mappedBy = "offering", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ClassSchedule> schedules = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "enrollment_code", nullable = false, unique = true, length = 16)
    private String enrollmentCode;

    @Column(name = "enrollment_code_expires_at")
    private LocalDateTime enrollmentCodeExpiresAt;
}