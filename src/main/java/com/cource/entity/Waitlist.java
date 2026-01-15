package com.cource.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@Entity
@Table(name = "waitlist", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "student_id", "offering_id" })
})
public class Waitlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering offering;

    @Column(nullable = false)
    private int position;

    @Column(name = "added_at", insertable = false, updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "status")
    private String status = "PENDING";

}