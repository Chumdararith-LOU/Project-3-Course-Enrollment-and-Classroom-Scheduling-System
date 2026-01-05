package com.cource.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "offering_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Builder.Default
    @Column(name = "status")
    private String status = "ENROLLED"; // Values: ENROLLED, DROPPED, COMPLETED, FAILED

    @Column(name = "grade")
    private String grade; // Values: A, B, C, D, F, W, I

}