package com.cource.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "attendance_codes", indexes = { @Index(columnList = "schedule_id") })
public class AttendanceCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "issued_at", nullable = false)
    private Long issuedAt; // epoch seconds

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "present_window_minutes")
    private Integer presentWindowMinutes;

    @Column(name = "late_window_minutes")
    private Integer lateWindowMinutes;

    public AttendanceCode() {
    }

    public AttendanceCode(Long scheduleId, String code, Long issuedAt, Long createdBy,
            Integer presentWindowMinutes, Integer lateWindowMinutes) {
        this.scheduleId = scheduleId;
        this.code = code;
        this.issuedAt = issuedAt;
        this.createdBy = createdBy;
        this.presentWindowMinutes = presentWindowMinutes;
        this.lateWindowMinutes = lateWindowMinutes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getPresentWindowMinutes() {
        return presentWindowMinutes;
    }

    public void setPresentWindowMinutes(Integer presentWindowMinutes) {
        this.presentWindowMinutes = presentWindowMinutes;
    }

    public Integer getLateWindowMinutes() {
        return lateWindowMinutes;
    }

    public void setLateWindowMinutes(Integer lateWindowMinutes) {
        this.lateWindowMinutes = lateWindowMinutes;
    }
}
