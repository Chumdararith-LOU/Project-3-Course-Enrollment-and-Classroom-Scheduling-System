package com.cource.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_codes", indexes = { @Index(columnList = "schedule_id") })
public class AttendanceCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ClassSchedule schedule;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "issued_at", nullable = false)
    private Long issuedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "present_window_minutes")
    private Integer presentWindowMinutes;

    @Column(name = "late_window_minutes")
    private Integer lateWindowMinutes;
}
