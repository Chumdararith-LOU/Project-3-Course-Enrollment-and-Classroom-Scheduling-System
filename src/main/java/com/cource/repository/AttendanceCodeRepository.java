package com.cource.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cource.entity.AttendanceCode;

public interface AttendanceCodeRepository extends JpaRepository<AttendanceCode, Long> {
    Optional<AttendanceCode> findByScheduleId(Long scheduleId);

    void deleteByScheduleId(Long scheduleId);
}
